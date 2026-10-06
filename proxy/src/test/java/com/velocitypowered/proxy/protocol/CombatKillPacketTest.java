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

package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.CombatKillPacket;
import com.velocitypowered.proxy.protocol.packet.EntityIdPayloadPacket;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class CombatKillPacketTest {
  @Test
  void deathUsesSelfIdHandlerAndPreservesMessageBytes() {
    for (var version : new ProtocolVersion[]{ProtocolVersion.MINECRAFT_1_21_11,
        ProtocolVersion.MINECRAFT_26_1, ProtocolVersion.MINECRAFT_26_2,
        ProtocolVersion.MINECRAFT_26_3}) {
      var input = Unpooled.buffer();
      var output = Unpooled.buffer();
      byte[] message = new byte[]{8, 0, 3, 100, 105, 101};
      try {
        ProtocolUtils.writeVarInt(input, 300);
        input.writeBytes(message);
        var registry = StateRegistry.PLAY.getProtocolRegistry(
            ProtocolUtils.Direction.CLIENTBOUND, version);
        var packet = registry.createPacket(registry.getPacketId(new CombatKillPacket()));
        packet.decode(input, ProtocolUtils.Direction.CLIENTBOUND, version);
        assertTrue(packet.handle(new MinecraftSessionHandler() {
          @Override
          public boolean handle(EntityIdPayloadPacket entity) {
            assertEquals(300, entity.getEntityId());
            entity.setEntityId(1);
            return true;
          }
        }));
        packet.encode(output, ProtocolUtils.Direction.CLIENTBOUND, version);
        assertEquals(1, ProtocolUtils.readVarInt(output));
        byte[] remaining = new byte[output.readableBytes()];
        output.readBytes(remaining);
        assertArrayEquals(message, remaining);
      } finally {
        input.release();
        output.release();
      }
    }
  }
}
