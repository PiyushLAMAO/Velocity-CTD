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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.EntityCameraPacket;
import com.velocitypowered.proxy.protocol.packet.ForgetChunkPacket;
import com.velocitypowered.proxy.protocol.packet.PlayerActionPacket;
import org.junit.jupiter.api.Test;

class TransferWorldPacketRegistryTest {
  @Test
  void packetIdsMatchPrimaryCodecsForEverySupportedClient() {
    ProtocolVersion[] versions = {
      ProtocolVersion.MINECRAFT_1_21_11,
      ProtocolVersion.MINECRAFT_26_1,
      ProtocolVersion.MINECRAFT_26_2,
      ProtocolVersion.MINECRAFT_26_3
    };
    int[] cameras = {0x5B, 0x5D, 0x5D, 0x5F};
    int[] forget = {0x25, 0x25, 0x25, 0x26};
    int[] actions = {0x29, 0x2A, 0x2A, 0x2A};
    for (int i = 0; i < versions.length; i++) {
      var cb =
          StateRegistry.PLAY.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND, versions[i]);
      var sb =
          StateRegistry.PLAY.getProtocolRegistry(ProtocolUtils.Direction.SERVERBOUND, versions[i]);
      assertEquals(cameras[i], cb.getPacketId(new EntityCameraPacket()));
      assertEquals(forget[i], cb.getPacketId(new ForgetChunkPacket()));
      assertEquals(actions[i], sb.getPacketId(new PlayerActionPacket()));
    }
  }
}
