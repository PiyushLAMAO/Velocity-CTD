/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.backend;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ClientboundSoundEntityPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class BackendSoundEntityRewriterTest {
  private static final ProtocolVersion[] VERSIONS = {
    ProtocolVersion.MINECRAFT_1_21_11, ProtocolVersion.MINECRAFT_26_1,
    ProtocolVersion.MINECRAFT_26_2, ProtocolVersion.MINECRAFT_26_3
  };

  private static ByteBuf packet(
      ProtocolVersion version, int emitter, int soundId, boolean range, int category) {
    ByteBuf input = Unpooled.buffer();
    var registry =
        StateRegistry.PLAY.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND, version);
    ProtocolUtils.writeVarInt(input, registry.getPacketId(new ClientboundSoundEntityPacket()));
    ProtocolUtils.writeVarInt(input, soundId);
    if (soundId == 0) {
      ProtocolUtils.writeString(input, "minecraft:block.cherry_wood_button.click_on");
      input.writeBoolean(range);
      if (range) {
        input.writeFloat(19.5f);
      }
    }
    ProtocolUtils.writeVarInt(input, category);
    ProtocolUtils.writeVarInt(input, emitter);
    input.writeFloat(0.3f).writeFloat(0.36f).writeLong(987654321987654321L);
    return input;
  }

  @Test
  void registeredAndCustomSoundBytesSurviveEmitterGrowthAndShrink() {
    for (var version : VERSIONS) {
      for (int soundId : new int[] {0, 1, 1170}) {
        for (boolean range : new boolean[] {false, true}) {
          for (int category : new int[] {0, 3}) {
            for (int[] ids : new int[][] {{300, Integer.MAX_VALUE}, {Integer.MAX_VALUE, 1}}) {
              ByteBuf input = packet(version, ids[0], soundId, range, category);
              ByteBuf expected = packet(version, ids[1], soundId, range, category);
              ByteBuf output = null;
              byte[] original = ByteBufUtil.getBytes(input);
              try {
                output = BackendSoundEntityRewriter.rewrite(input, version, ids[0], ids[1]);
                assertNotNull(output);
                assertArrayEquals(ByteBufUtil.getBytes(expected), ByteBufUtil.getBytes(output));
                assertArrayEquals(original, ByteBufUtil.getBytes(input));
                assertEquals(0, input.readerIndex());
                assertEquals(1, input.refCnt());
                assertEquals(1, output.refCnt());
              } finally {
                input.release();
                expected.release();
                if (output != null) {
                  output.release();
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  void unrelatedEntityAndUnchangedOrUnavailableIdentityPassThrough() {
    for (var version : VERSIONS) {
      ByteBuf input = packet(version, 301, 1170, false, 0);
      try {
        byte[] original = ByteBufUtil.getBytes(input);
        assertNull(BackendSoundEntityRewriter.rewrite(input, version, 300, Integer.MAX_VALUE));
        assertNull(BackendSoundEntityRewriter.rewrite(input, version, 301, 301));
        assertNull(BackendSoundEntityRewriter.rewrite(input, version, null, Integer.MAX_VALUE));
        assertNull(BackendSoundEntityRewriter.rewrite(input, version, 301, null));
        assertArrayEquals(original, ByteBufUtil.getBytes(input));
      } finally {
        input.release();
      }
    }
  }

  @Test
  void otherRawPacketsAndOlderProtocolsRemainUntouched() {
    ByteBuf input = Unpooled.buffer().writeByte(0x01).writeInt(12345);
    try {
      byte[] original = ByteBufUtil.getBytes(input);
      for (var version : VERSIONS) {
        assertNull(BackendSoundEntityRewriter.rewrite(input, version, 300, Integer.MAX_VALUE));
      }
      assertNull(
          BackendSoundEntityRewriter.rewrite(
              input, ProtocolVersion.MINECRAFT_1_8, 300, Integer.MAX_VALUE));
      assertArrayEquals(original, ByteBufUtil.getBytes(input));
    } finally {
      input.release();
    }
  }

  @Test
  void truncatedSelfSoundFailsWithoutChangingInputOrLeakingOutput() {
    ByteBuf input = packet(ProtocolVersion.MINECRAFT_26_3, 300, 1170, false, 0);
    try {
      input.writerIndex(input.writerIndex() - 1);
      byte[] original = ByteBufUtil.getBytes(input);
      assertThrows(
          IllegalArgumentException.class,
          () ->
              BackendSoundEntityRewriter.rewrite(
                  input, ProtocolVersion.MINECRAFT_26_3, 300, Integer.MAX_VALUE));
      assertArrayEquals(original, ByteBufUtil.getBytes(input));
      assertEquals(1, input.refCnt());
    } finally {
      input.release();
    }
  }

  @Test
  void nonzeroReaderIndexAndSlicedFramesPreserveBorrowedStorage() {
    ByteBuf storage = Unpooled.buffer().writeLong(42);
    ByteBuf sound = packet(ProtocolVersion.MINECRAFT_1_21_11, 300, 0, true, 3);
    ByteBuf expected = packet(ProtocolVersion.MINECRAFT_1_21_11, Integer.MAX_VALUE, 0, true, 3);
    ByteBuf output = null;
    try {
      storage.writeBytes(sound);
      storage.readerIndex(8);
      output =
          BackendSoundEntityRewriter.rewrite(
              storage.slice(), ProtocolVersion.MINECRAFT_1_21_11, 300, Integer.MAX_VALUE);
      assertArrayEquals(ByteBufUtil.getBytes(expected), ByteBufUtil.getBytes(output));
      assertEquals(8, storage.readerIndex());
      assertEquals(42, storage.getLong(0));
    } finally {
      storage.release();
      sound.release();
      expected.release();
      if (output != null) {
        output.release();
      }
    }
  }
}
