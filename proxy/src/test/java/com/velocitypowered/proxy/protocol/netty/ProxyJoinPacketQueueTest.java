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

package com.velocitypowered.proxy.protocol.netty;

import static org.junit.jupiter.api.Assertions.*;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.ServerboundPlayerLoadedPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

class ProxyJoinPacketQueueTest {
  @Test
  void arrivalIsHeldButKeepalivesAndMovementStillReachVirtualHoldingServer() {
    var hold = new ProxyJoinPacketQueue(ProtocolVersion.MINECRAFT_26_3);
    var channel = new EmbeddedChannel();
    channel.pipeline().addLast(ProxyJoinPacketQueue.NAME, hold);
    var promise = channel.newPromise();
    channel.write(ServerboundPlayerLoadedPacket.INSTANCE, promise);
    channel.flush();
    assertNull(channel.readOutbound());
    assertFalse(promise.isDone());
    var keepalive = new KeepAlivePacket();
    channel.writeOutbound(keepalive);
    assertSame(keepalive, channel.readOutbound());
    var movement = Unpooled.buffer().writeByte(9);
    channel.writeOutbound(movement);
    assertSame(movement, channel.readOutbound());
    movement.release();
    hold.complete(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
    assertSame(ServerboundPlayerLoadedPacket.INSTANCE, channel.readOutbound());
    assertTrue(promise.isSuccess());
    assertNull(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
    channel.finishAndReleaseAll();
  }

  @Test
  void closingBeforeArrivalFailsHeldWritesRatherThanSilentlyLosingCommands() {
    var channel = new EmbeddedChannel();
    channel.pipeline().addLast(ProxyJoinPacketQueue.NAME,
        new ProxyJoinPacketQueue(ProtocolVersion.MINECRAFT_1_21_11));
    var promise = channel.newPromise();
    channel.write(ServerboundPlayerLoadedPacket.INSTANCE, promise);
    channel.close();
    assertTrue(promise.isDone());
    assertFalse(promise.isSuccess());
    assertNull(channel.readOutbound());
    channel.finishAndReleaseAll();
  }
  @Test
  void proxyPublicationReleasesArrivalButCommandsWaitForActualGameReply() {
    for (var version : new ProtocolVersion[]{ProtocolVersion.MINECRAFT_1_21_11, ProtocolVersion.MINECRAFT_26_3}) {
      var hold = new ProxyJoinPacketQueue(version);
      var channel = new EmbeddedChannel();
      channel.pipeline().addLast(ProxyJoinPacketQueue.NAME, hold);
      var command = new com.velocitypowered.proxy.protocol.packet.chat.session.UnsignedPlayerCommandPacket();
      var data = Unpooled.buffer();
      com.velocitypowered.proxy.protocol.ProtocolUtils.writeString(data, "settings");
      command.decode(data, com.velocitypowered.proxy.protocol.ProtocolUtils.Direction.SERVERBOUND, version);
      data.release();
      var promise = channel.newPromise();
      channel.writeOutbound(ServerboundPlayerLoadedPacket.INSTANCE);
      channel.write(command, promise);
      channel.flush();
      assertNull(channel.readOutbound());
      hold.proxyReady(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
      assertSame(ServerboundPlayerLoadedPacket.INSTANCE, channel.readOutbound());
      var probe = (com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket) channel.readOutbound();
      assertEquals(ProxyJoinPacketQueue.PROBE_TRANSACTION, probe.getTransactionId());
      assertEquals("/", probe.getCommand());
      assertFalse(promise.isDone());
      assertNull(channel.readOutbound());
      hold.complete(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
      assertSame(ServerboundPlayerLoadedPacket.INSTANCE, channel.readOutbound());
      assertSame(command, channel.readOutbound());
      assertTrue(promise.isSuccess());
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void commandsAfterProxyPublicationAreStillHeldUntilGameReply() {
    var hold = new ProxyJoinPacketQueue(ProtocolVersion.MINECRAFT_26_3);
    var channel = new EmbeddedChannel();
    channel.pipeline().addLast(ProxyJoinPacketQueue.NAME, hold);
    hold.proxyReady(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
    assertInstanceOf(com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket.class, channel.readOutbound());
    channel.writeOutbound(ServerboundPlayerLoadedPacket.INSTANCE);
    assertSame(ServerboundPlayerLoadedPacket.INSTANCE, channel.readOutbound());
    var command = new com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket();
    command.setCommand("/settings");
    var promise = channel.newPromise();
    channel.write(command, promise);
    channel.flush();
    assertNull(channel.readOutbound());
    hold.complete(channel.pipeline().context(ProxyJoinPacketQueue.NAME));
    assertSame(ServerboundPlayerLoadedPacket.INSTANCE, channel.readOutbound());
    assertSame(command, channel.readOutbound());
    assertTrue(promise.isSuccess());
    channel.advanceTimeBy(1, java.util.concurrent.TimeUnit.SECONDS);
    channel.runScheduledPendingTasks();
    assertNull(channel.readOutbound());
    channel.finishAndReleaseAll();
  }

}
