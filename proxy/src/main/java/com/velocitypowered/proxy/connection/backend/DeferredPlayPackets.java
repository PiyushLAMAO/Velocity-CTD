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

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Queue;

/** Retains the already-decoded tail of a network read while JoinGame's event is pending. */
final class DeferredPlayPackets {
  private final Queue<Object> packets = new ArrayDeque<>();

  void add(Object packet) {
    packets.add(ReferenceCountUtil.retain(packet));
  }

  void replay(MinecraftSessionHandler handler) {
    try {
      Object packet;
      while ((packet = packets.poll()) != null) {
        try {
          if (handler.beforeHandle()) {
            continue;
          }
          if (packet instanceof MinecraftPacket decoded) {
            if (!decoded.handle(handler)) {
              handler.handleGeneric(decoded);
            }
          } else {
            handler.handleUnknown((ByteBuf) packet);
          }
        } finally {
          ReferenceCountUtil.release(packet);
        }
      }
      handler.readCompleted();
    } finally {
      clear();
    }
  }

  void clear() {
    Object packet;
    while ((packet = packets.poll()) != null) {
      ReferenceCountUtil.release(packet);
    }
  }
}
