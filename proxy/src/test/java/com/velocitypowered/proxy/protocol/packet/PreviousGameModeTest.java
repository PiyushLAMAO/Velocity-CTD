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

package com.velocitypowered.proxy.protocol.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/**
 * Official 26.3 GameType.OPTIONAL_STREAM_CODEC uses OPTIONAL_VAR_INT; 1.21.11 uses a signed byte.
 */
class PreviousGameModeTest {
  @Test
  void nativeAndOlderJoinWireAndCanonicalMode() throws ReflectiveOperationException {
    for (var version :
        new ProtocolVersion[] {ProtocolVersion.MINECRAFT_26_3, ProtocolVersion.MINECRAFT_1_21_11}) {
      for (int mode = -1; mode <= 3; mode++) {
        JoinGamePacket packet = new JoinGamePacket();
        var levelsField = JoinGamePacket.class.getDeclaredField("levelNames");
        levelsField.setAccessible(true);
        levelsField.set(packet, ImmutableSet.of("minecraft:overworld"));
        packet.setDimensionInfo(
            new DimensionInfo("minecraft:overworld", "minecraft:overworld", false, false, version));
        packet.setGamemode(2);
        packet.setPreviousGamemode(mode);
        ByteBuf buffer = Unpooled.buffer();
        try {
          packet.encode(buffer, ProtocolUtils.Direction.CLIENTBOUND, version);
          ByteBuf cursor = buffer.duplicate();
          cursor.readInt();
          cursor.readBoolean();
          int levels = ProtocolUtils.readVarInt(cursor);
          for (int i = 0; i < levels; i++) {
            ProtocolUtils.readString(cursor);
          }
          ProtocolUtils.readVarInt(cursor);
          ProtocolUtils.readVarInt(cursor);
          ProtocolUtils.readVarInt(cursor);
          cursor.skipBytes(3);
          ProtocolUtils.readVarInt(cursor);
          ProtocolUtils.readString(cursor);
          cursor.readLong();
          assertEquals(2, ProtocolUtils.readVarInt(cursor));
          assertEquals(
              version == ProtocolVersion.MINECRAFT_26_3 ? mode + 1 : mode,
              version == ProtocolVersion.MINECRAFT_26_3
                  ? ProtocolUtils.readVarInt(cursor)
                  : cursor.readByte());
          JoinGamePacket decoded = new JoinGamePacket();
          decoded.decode(buffer, ProtocolUtils.Direction.CLIENTBOUND, version);
          assertEquals(mode, decoded.getPreviousGamemode());
          assertEquals(0, buffer.readableBytes());
        } finally {
          buffer.release();
        }
      }
    }
  }

  @Test
  void nativeRespawnOptionalModeRoundTrip() throws ReflectiveOperationException {
    for (int mode = -1; mode <= 3; mode++) {
      var version = ProtocolVersion.MINECRAFT_26_3;
      RespawnPacket packet = new RespawnPacket();
      var dimensionField = RespawnPacket.class.getDeclaredField("dimensionInfo");
      dimensionField.setAccessible(true);
      dimensionField.set(
          packet,
          new DimensionInfo("minecraft:overworld", "minecraft:overworld", false, false, version));
      packet.setGamemode(2);
      packet.setPreviousGamemode(mode);
      ByteBuf buffer = Unpooled.buffer();
      try {
        packet.encode(buffer, ProtocolUtils.Direction.CLIENTBOUND, version);
        ByteBuf cursor = buffer.duplicate();
        ProtocolUtils.readVarInt(cursor);
        ProtocolUtils.readString(cursor);
        cursor.readLong();
        assertEquals(2, ProtocolUtils.readVarInt(cursor));
        assertEquals(mode + 1, ProtocolUtils.readVarInt(cursor));
        RespawnPacket decoded = new RespawnPacket();
        decoded.decode(buffer, ProtocolUtils.Direction.CLIENTBOUND, version);
        assertEquals(mode, decoded.getPreviousGamemode());
        assertEquals(0, buffer.readableBytes());
      } finally {
        buffer.release();
      }
    }
  }
}
