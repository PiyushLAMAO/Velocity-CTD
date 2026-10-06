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

package com.velocitypowered.proxy.connection.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.ForgetChunkPacket;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientWorldChunksTest {
  @Test
  void unloadsOldWorldOnceAndPreservesRawReaderIndexForAllSupportedVersions() {
    ProtocolVersion[] versions = {
      ProtocolVersion.MINECRAFT_1_21_11,
      ProtocolVersion.MINECRAFT_26_1,
      ProtocolVersion.MINECRAFT_26_2,
      ProtocolVersion.MINECRAFT_26_3
    };
    int[] ids = {0x2C, 0x2D, 0x2D, 0x2E};
    for (int i = 0; i < versions.length; i++) {
      var chunks = new ClientWorldChunks();
      var buf = Unpooled.buffer();
      ProtocolUtils.writeVarInt(buf, ids[i]);
      buf.writeInt(-3).writeInt(8).writeLong(12345); // opaque chunk payload remains untouched.
      chunks.observe(buf, versions[i]);
      chunks.observe(buf, versions[i]);
      assertEquals(0, buf.readerIndex());
      List<ForgetChunkPacket> forgotten = new ArrayList<>();
      chunks.unload(forgotten::add);
      chunks.unload(forgotten::add);
      assertEquals(1, forgotten.size());
      long position = (-3 & 0xffffffffL) | (8L << 32);
      assertEquals(position, forgotten.getFirst().getPosition());
      var wire = Unpooled.buffer();
      forgotten.getFirst().encode(wire, ProtocolUtils.Direction.CLIENTBOUND, versions[i]);
      assertEquals(8, wire.readInt());
      assertEquals(-3, wire.readInt());
      wire.release();
      buf.release();
    }
  }

  @Test
  void normalForgetAndConfigurationResetDiscardOldCoordinates() {
    var chunks = new ClientWorldChunks();
    var buf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(buf, 0x2E);
    buf.writeInt(4).writeInt(-9);
    chunks.observe(buf, ProtocolVersion.MINECRAFT_26_3);
    chunks.forget(4L | (-9L << 32));
    List<ForgetChunkPacket> forgotten = new ArrayList<>();
    chunks.unload(forgotten::add);
    assertTrue(forgotten.isEmpty());
    chunks.observe(buf, ProtocolVersion.MINECRAFT_26_3);
    chunks.clear();
    chunks.unload(forgotten::add);
    assertTrue(forgotten.isEmpty());
    buf.release();
  }
}
