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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class TransitionArrivalOrderTest {
  @Test
  void forwardingPhaseIsPublishedBeforeClientWorldFlush() {
    var server = mock(VelocityServer.class, RETURNS_DEEP_STUBS);
    var player = mock(ConnectedPlayer.class);
    var destination = mock(VelocityServerConnection.class, RETURNS_DEEP_STUBS);
    var frontend = mock(MinecraftConnection.class);
    var backend = mock(MinecraftConnection.class);
    var play = mock(ClientPlaySessionHandler.class);
    var channel = new EmbeddedChannel();
    when(destination.getPlayer()).thenReturn(player);
    when(destination.isActive()).thenReturn(true);
    when(destination.ensureConnected()).thenReturn(backend);
    when(destination.getPreviousServer()).thenReturn(Optional.empty());
    when(destination.getProxyAdmission()).thenReturn(null);
    when(player.getConnection()).thenReturn(frontend);
    when(frontend.getActiveSessionHandler()).thenReturn(play);
    when(backend.getChannel()).thenReturn(channel);
    when(backend.eventLoop()).thenReturn(channel.eventLoop());
    when(backend.getProtocolVersion()).thenReturn(ProtocolVersion.MINECRAFT_26_3);
    when(server.getEventManager().fire(any(ServerConnectedEvent.class)))
        .thenAnswer(call -> CompletableFuture.completedFuture(call.getArgument(0)));
    var result = new CompletableFuture<Impl>();
    var handler = new TransitionSessionHandler(server, destination, result);
    var join = new JoinGamePacket();
    handler.handle(join);
    channel.runPendingTasks();
    var order = inOrder(player, destination, play);
    order.verify(player).setConnectedServer(destination);
    order.verify(destination).completeJoin();
    order.verify(play).handleBackendJoinGame(join, destination);
    org.junit.jupiter.api.Assertions.assertTrue(result.isDone());
    org.junit.jupiter.api.Assertions.assertFalse(result.isCompletedExceptionally());
    channel.finishAndReleaseAll();
  }
}
