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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ServerboundPlayerLoadedPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Holds initial PLAY actions until the admitted downstream gameplay listener replies. */
public final class ProxyJoinPacketQueue extends ChannelDuplexHandler {
  private static final Logger LOGGER = LogManager.getLogger(ProxyJoinPacketQueue.class);
  private static final boolean TRACE = Boolean.getBoolean("insane.gateway.trace");
  public static final String NAME = "proxy-join-packet-queue";
  public static final String READY_CHANNEL = "insane:route_ready";
  public static final int PROBE_TRANSACTION = Integer.MAX_VALUE - 1;
  private final StateRegistry.PacketRegistry.ProtocolRegistry configuration;
  private final Queue<Pending> pending = new ArrayDeque<>();
  private boolean ready;
  private boolean proxyReady;
  private int attempts;
  private ScheduledFuture<?> retry;

  public ProxyJoinPacketQueue(ProtocolVersion version) {
    configuration = StateRegistry.CONFIG.getProtocolRegistry(ProtocolUtils.Direction.SERVERBOUND, version);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
    // Keep movement flowing to virtual holding servers. CONFIG acknowledgements and keepalives
    // must never depend on a gameplay backend being online.
    if (ready || !(message instanceof MinecraftPacket packet)
        || configuration.containsPacket(packet)
        || (proxyReady && message instanceof ServerboundPlayerLoadedPacket)) {
      ctx.write(message, promise);
      return;
    }
    if (pending.size() >= 1024) {
      ReferenceCountUtil.release(message);
      promise.tryFailure(new IllegalStateException("Too many actions before downstream arrival"));
      ctx.close();
      return;
    }
    pending.add(new Pending(message, promise));
    if (TRACE) {
      LOGGER.info("Gateway arrival held {} ({} pending)", message.getClass().getSimpleName(), pending.size());
    }
  }

  /** Allows arrival acknowledgements, then asks the actual game listener for a read-only reply. */
  public void proxyReady(ChannelHandlerContext ctx) {
    if (proxyReady || ready) {
      return;
    }
    proxyReady = true;
    for (var iterator = pending.iterator(); iterator.hasNext();) {
      Pending packet = iterator.next();
      if (packet.message() instanceof ServerboundPlayerLoadedPacket) {
        iterator.remove();
        ctx.write(packet.message(), packet.promise());
      }
    }
    probe(ctx);
  }

  private void probe(ChannelHandlerContext ctx) {
    if (ready || !ctx.channel().isActive()) {
      return;
    }
    if (attempts++ >= 50) {
      LOGGER.warn("Downstream gameplay did not acknowledge arrival");
      ctx.close();
      return;
    }
    var packet = new TabCompleteRequestPacket();
    packet.setTransactionId(PROBE_TRANSACTION);
    packet.setCommand("/");
    // Bypass this queue for the probe; it never executes a command or changes player state.
    ctx.writeAndFlush(packet);
    retry = ctx.executor().schedule(() -> probe(ctx), 100, TimeUnit.MILLISECONDS);
  }

  public void complete(ChannelHandlerContext ctx) {
    if (ready) {
      return;
    }
    ready = true;
    cancelRetry();
    if (proxyReady) {
      // The first acknowledgement may have reached a backend still finishing its own join.
      // This reply proves its game listener is live; acknowledge again before queued actions.
      ctx.write(ServerboundPlayerLoadedPacket.INSTANCE);
    }
    if (TRACE) {
      LOGGER.info("Gateway destination ready: releasing {} actions", pending.size());
    }
    Pending packet;
    while ((packet = pending.poll()) != null) {
      ctx.write(packet.message(), packet.promise());
    }
    ctx.flush();
    ctx.pipeline().remove(this);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    discard();
    super.channelInactive(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    discard();
  }

  private void discard() {
    cancelRetry();
    Pending packet;
    while ((packet = pending.poll()) != null) {
      ReferenceCountUtil.release(packet.message());
      packet.promise().tryFailure(new IllegalStateException("Downstream closed before arrival"));
    }
  }

  private void cancelRetry() {
    if (retry != null) {
      retry.cancel(false);
      retry = null;
    }
  }

  private record Pending(Object message, ChannelPromise promise) { }
}
