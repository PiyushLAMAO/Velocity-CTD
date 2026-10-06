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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class RepeatedConfigurationTest {
  @Test
  void sameSocketAcknowledgesEachConfigurationCycleExactlyOnce() throws Exception {
    var server = mock(VelocityServer.class);
    var destination = mock(VelocityServerConnection.class);
    var player = mock(ConnectedPlayer.class, RETURNS_DEEP_STUBS);
    var connection = mock(MinecraftConnection.class);
    when(destination.getServerInfo()).thenReturn(new ServerInfo("economy-route",
        new InetSocketAddress("127.0.0.1", 25690)));
    when(destination.getConnection()).thenReturn(connection);
    when(destination.getPlayer()).thenReturn(player);
    when(player.getConnectedServer()).thenReturn(destination);
    when(player.getProtocolVersion()).thenReturn(ProtocolVersion.MINECRAFT_1_20_5);
    doReturn(net.kyori.adventure.text.Component.empty()).when(player).getPlayerListHeader();
    doReturn(net.kyori.adventure.text.Component.empty()).when(player).getPlayerListFooter();
    var config = new ConfigSessionHandler(server, destination, new CompletableFuture<>());
    var advance = ConfigSessionHandler.class.getDeclaredMethod("advanceBackendToPlay", boolean.class);
    advance.setAccessible(true);
    config.activated();
    advance.invoke(config, false);
    advance.invoke(config, false);
    verify(connection).write(FinishedUpdatePacket.INSTANCE);
    config.activated();
    advance.invoke(config, false);
    advance.invoke(config, false);
    verify(connection, times(2)).write(FinishedUpdatePacket.INSTANCE);
  }

  @Test
  void reconfiguredJoinUsesTheNewClientPlayHandlerAndItsPlayerIdentity() {
    var server = mock(VelocityServer.class);
    var destination = mock(VelocityServerConnection.class);
    var player = mock(ConnectedPlayer.class);
    var connection = mock(MinecraftConnection.class);
    var oldPlay = mock(ClientPlaySessionHandler.class);
    var newPlay = mock(ClientPlaySessionHandler.class);
    when(destination.getPlayer()).thenReturn(player);
    when(player.getConnection()).thenReturn(connection);
    when(connection.getActiveSessionHandler()).thenReturn(oldPlay);
    var handler = new BackendPlaySessionHandler(server, destination);
    when(connection.getActiveSessionHandler()).thenReturn(newPlay);
    var join = new JoinGamePacket();
    assertTrue(handler.handle(join));
    verify(newPlay).handleBackendJoinGame(join, destination);
    verifyNoInteractions(oldPlay);
  }
}
