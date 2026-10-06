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

package com.velocitypowered.proxy.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForwardedRemoteAddressTest {
  private MinecraftConnection connection;
  private EventLoop loop;
  private final InetSocketAddress transport = new InetSocketAddress("17.18.0.1", 33524);

  @BeforeEach
  void setup() {
    Channel channel = mock(Channel.class);
    loop = mock(EventLoop.class);
    when(channel.eventLoop()).thenReturn(loop);
    when(loop.inEventLoop()).thenReturn(true);
    when(channel.remoteAddress()).thenReturn(transport);
    connection = new MinecraftConnection(channel, mock(VelocityServer.class), UUID.randomUUID());
  }

  @Test
  void authenticatedAddressReplacesTransportForAllAddressReaders() {
    InetSocketAddress actual = new InetSocketAddress("203.0.113.91", 54231);
    assertEquals(transport, connection.getRemoteAddress());
    connection.setForwardedRemoteAddress(actual);
    assertEquals(actual, connection.getRemoteAddress());
  }

  @Test
  void rejectsUnresolvedAndMissingAddressesWithoutChangingTransport() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            connection.setForwardedRemoteAddress(
                InetSocketAddress.createUnresolved("untrusted.example", 1234)));
    assertThrows(
        IllegalArgumentException.class,
        () -> connection.setForwardedRemoteAddress(new InetSocketAddress("203.0.113.91", 0)));
    assertThrows(NullPointerException.class, () -> connection.setForwardedRemoteAddress(null));
    assertEquals(transport, connection.getRemoteAddress());
  }

  @Test
  void refusesMutationOutsideConnectionEventLoop() {
    when(loop.inEventLoop()).thenReturn(false);
    assertThrows(
        IllegalStateException.class,
        () -> connection.setForwardedRemoteAddress(new InetSocketAddress("203.0.113.91", 54231)));
    assertEquals(transport, connection.getRemoteAddress());
  }
}
