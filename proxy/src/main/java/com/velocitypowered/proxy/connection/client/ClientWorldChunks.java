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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.ForgetChunkPacket;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Tracks chunk coordinates only; no chunk payload or NBT is retained. */
public final class ClientWorldChunks {
  private final Set<Long> positions = ConcurrentHashMap.newKeySet();

  public void observe(ByteBuf packet, ProtocolVersion version) {
    if (version.lessThan(ProtocolVersion.MINECRAFT_1_21_11)) {
      return;
    }
    ByteBuf header = packet.duplicate();
    int packetId = ProtocolUtils.readVarInt(header);
    int chunkId = version.noLessThan(ProtocolVersion.MINECRAFT_26_3) ? 0x2E
        : version.noLessThan(ProtocolVersion.MINECRAFT_26_1) ? 0x2D : 0x2C;
    if (packetId == chunkId) {
      int x = header.readInt();
      int z = header.readInt();
      positions.add((x & 0xffffffffL) | ((long) z << 32));
    }
  }

  public void forget(long position) {
    positions.remove(position);
  }

  public void unload(Consumer<ForgetChunkPacket> send) {
    for (long position : positions) {
      send.accept(new ForgetChunkPacket(position));
    }
    positions.clear();
  }

  public void clear() {
    positions.clear();
  }
}
