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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.packet.DamageEventPacket;
import com.velocitypowered.proxy.protocol.packet.EntityAnimationPacket;
import com.velocitypowered.proxy.protocol.packet.EntityEffectPacket;
import com.velocitypowered.proxy.protocol.packet.EntityEventPacket;
import com.velocitypowered.proxy.protocol.packet.EntityMetadataPacket;
import com.velocitypowered.proxy.protocol.packet.EntityVelocityPacket;
import com.velocitypowered.proxy.protocol.packet.GameEventPacket;
import com.velocitypowered.proxy.protocol.packet.HurtAnimationPacket;
import com.velocitypowered.proxy.protocol.packet.RemoveEntitiesPacket;
import com.velocitypowered.proxy.protocol.packet.RemoveEntityEffectPacket;
import com.velocitypowered.proxy.protocol.packet.SpawnEntityPacket;
import com.velocitypowered.proxy.protocol.packet.UpdateAttributesPacket;
import org.junit.jupiter.api.Test;

/**
 * Exact IDs from the local Mojang-mapped 26.3 GameProtocols.CLIENTBOUND_TEMPLATE.
 * A misplaced codec would otherwise corrupt ordinary play packets for every client.
 */
class SeamlessPacketRegistryTest {
  @Test
  void translated1_21_11EntityAndGameEventIds() {
    var registry = StateRegistry.PLAY.getProtocolRegistry(Direction.CLIENTBOUND,
        ProtocolVersion.MINECRAFT_1_21_11);
    assertEquals(0x01, registry.getPacketId(new SpawnEntityPacket()));
    assertEquals(0x02, registry.getPacketId(new EntityAnimationPacket()));
    assertEquals(0x19, registry.getPacketId(new DamageEventPacket()));
    assertEquals(0x22, registry.getPacketId(new EntityEventPacket()));
    assertEquals(0x26, registry.getPacketId(new GameEventPacket()));
    assertEquals(0x29, registry.getPacketId(new HurtAnimationPacket()));
    assertEquals(0x4B, registry.getPacketId(new RemoveEntitiesPacket()));
    assertEquals(0x4C, registry.getPacketId(new RemoveEntityEffectPacket()));
    assertEquals(0x61, registry.getPacketId(new EntityMetadataPacket()));
    assertEquals(0x63, registry.getPacketId(new EntityVelocityPacket()));
    assertEquals(0x81, registry.getPacketId(new UpdateAttributesPacket()));
    assertEquals(0x82, registry.getPacketId(new EntityEffectPacket()));
  }

  @Test
  void native26_3EntityAndGameEventIds() {
    var registry = StateRegistry.PLAY.getProtocolRegistry(Direction.CLIENTBOUND,
        ProtocolVersion.MINECRAFT_26_3);
    assertEquals(0x01, registry.getPacketId(new SpawnEntityPacket()));
    assertEquals(0x02, registry.getPacketId(new EntityAnimationPacket()));
    assertEquals(0x19, registry.getPacketId(new DamageEventPacket()));
    assertEquals(0x22, registry.getPacketId(new EntityEventPacket()));
    assertEquals(0x27, registry.getPacketId(new GameEventPacket()));
    assertEquals(0x2B, registry.getPacketId(new HurtAnimationPacket()));
    assertEquals(0x4E, registry.getPacketId(new RemoveEntitiesPacket()));
    assertEquals(0x4F, registry.getPacketId(new RemoveEntityEffectPacket()));
    assertEquals(0x65, registry.getPacketId(new EntityMetadataPacket()));
    assertEquals(0x67, registry.getPacketId(new EntityVelocityPacket()));
    assertEquals(0x86, registry.getPacketId(new UpdateAttributesPacket()));
    assertEquals(0x87, registry.getPacketId(new EntityEffectPacket()));
  }
}
