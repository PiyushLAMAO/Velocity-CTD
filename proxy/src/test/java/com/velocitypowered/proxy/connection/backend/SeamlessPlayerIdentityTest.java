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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientEntityIds;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.EntityCameraPacket;
import com.velocitypowered.proxy.protocol.packet.RemoveEntitiesPacket;
import com.velocitypowered.proxy.protocol.packet.SpawnEntityPacket;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SeamlessPlayerIdentityTest {
  @Test
  void ordinaryEntityThatUsedTheOldPlayerIdCannotRemoveTheRetainedPlayer() {
    var server = mock(VelocityServer.class);
    var destination = mock(VelocityServerConnection.class);
    var player = mock(ConnectedPlayer.class);
    var connection = mock(MinecraftConnection.class);
    var play = mock(ClientPlaySessionHandler.class);
    var tracked = new HashSet<Integer>();
    when(destination.getPlayer()).thenReturn(player);
    when(destination.getEntityId()).thenReturn(27);
    when(player.getConnection()).thenReturn(connection);
    when(connection.getActiveSessionHandler()).thenReturn(play);
    when(play.getClientEntityId()).thenReturn(ClientEntityIds.SELF);
    when(play.getTrackedEntityIds()).thenReturn(tracked);
    var handler = new BackendPlaySessionHandler(server, destination);
    var destinationEntity = mock(SpawnEntityPacket.class);
    when(destinationEntity.getEntityId()).thenReturn(9); // Previously the client's Hub self ID.
    assertFalse(handler.handle(destinationEntity));
    assertEquals(java.util.Set.of(9), tracked);
    assertFalse(handler.handle(new RemoveEntitiesPacket(List.of(9))));
    assertTrue(tracked.isEmpty());
    var camera = new EntityCameraPacket();
    camera.setEntityId(27);
    assertFalse(handler.handle(camera));
    assertEquals(ClientEntityIds.SELF, camera.getEntityId());
    var attemptedSelf = mock(SpawnEntityPacket.class);
    when(attemptedSelf.getEntityId()).thenReturn(ClientEntityIds.SELF);
    assertTrue(handler.handle(attemptedSelf));
    assertTrue(tracked.isEmpty());
    assertTrue(handler.handle(new RemoveEntitiesPacket(List.of(ClientEntityIds.SELF, 9))));
    var forwarded = ArgumentCaptor.forClass(RemoveEntitiesPacket.class);
    verify(connection).delayedWrite(forwarded.capture());
    assertEquals(List.of(9), forwarded.getValue().getEntityIds());
  }

  @Test
  void onlySupportedClientsUseTheReservedFrontendPlayerId() {
    for (var version :
        new ProtocolVersion[] {
          ProtocolVersion.MINECRAFT_1_21_11,
          ProtocolVersion.MINECRAFT_26_1,
          ProtocolVersion.MINECRAFT_26_2,
          ProtocolVersion.MINECRAFT_26_3
        }) {
      assertEquals(ClientEntityIds.SELF, ClientEntityIds.forJoin(version, 9));
    }
    assertEquals(9, ClientEntityIds.forJoin(ProtocolVersion.MINECRAFT_1_20_2, 9));
  }
}
