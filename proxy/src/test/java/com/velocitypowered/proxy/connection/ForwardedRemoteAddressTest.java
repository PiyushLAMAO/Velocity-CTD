package com.velocitypowered.proxy.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    assertThrows(IllegalArgumentException.class, () -> connection.setForwardedRemoteAddress(
        InetSocketAddress.createUnresolved("untrusted.example", 1234)));
    assertThrows(IllegalArgumentException.class, () -> connection.setForwardedRemoteAddress(
        new InetSocketAddress("203.0.113.91", 0)));
    assertThrows(NullPointerException.class, () -> connection.setForwardedRemoteAddress(null));
    assertEquals(transport, connection.getRemoteAddress());
  }

  @Test
  void refusesMutationOutsideConnectionEventLoop() {
    when(loop.inEventLoop()).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> connection.setForwardedRemoteAddress(
        new InetSocketAddress("203.0.113.91", 54231)));
    assertEquals(transport, connection.getRemoteAddress());
  }
}
