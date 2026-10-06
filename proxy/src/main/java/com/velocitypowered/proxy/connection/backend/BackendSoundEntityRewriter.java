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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ClientboundSoundEntityPacket;
import io.netty.buffer.ByteBuf;

/** Translates retained-player references in raw sounds and 26.3 passenger packets. */
public final class BackendSoundEntityRewriter {
  private BackendSoundEntityRewriter() { }

  /** Transfers one owned reference to the outbound connection. */
  public static ByteBuf forward(ByteBuf input, ProtocolVersion version,
      Integer backendEntityId, Integer clientEntityId) {
    ByteBuf rewritten = rewrite(input, version, backendEntityId, clientEntityId);
    return rewritten == null ? input.retain() : rewritten;
  }

  /** Returns an owned replacement, or null when the borrowed input can be forwarded unchanged. */
  public static ByteBuf rewrite(ByteBuf input, ProtocolVersion version,
      Integer backendEntityId, Integer clientEntityId) {
    ByteBuf passengers = BackendPassengerEntityRewriter.rewrite(
        input, version, backendEntityId, clientEntityId);
    if (passengers != null) {
      return passengers;
    }
    if (backendEntityId == null || clientEntityId == null
        || backendEntityId.equals(clientEntityId) || !input.isReadable()) {
      return null;
    }
    var registry = StateRegistry.PLAY.getProtocolRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, version);
    var soundPacket = new ClientboundSoundEntityPacket();
    if (!registry.containsPacket(soundPacket)) {
      return null;
    }
    ByteBuf reader = input.duplicate();
    if (ProtocolUtils.readVarInt(reader) != registry.getPacketId(soundPacket)) {
      return null;
    }
    // A positive sound holder is a registry ID. Zero carries a key and optional fixed range.
    int soundId = ProtocolUtils.readVarInt(reader);
    if (soundId == 0) {
      ProtocolUtils.readString(reader);
      if (reader.readBoolean()) {
        reader.skipBytes(Float.BYTES);
      }
    }
    ProtocolUtils.readVarInt(reader); // Sound category remains byte-for-byte unchanged.
    int emitterStart = reader.readerIndex();
    int emitter = ProtocolUtils.readVarInt(reader);
    if (emitter != backendEntityId) {
      return null;
    }
    if (reader.readableBytes() != 2 * Float.BYTES + Long.BYTES) {
      throw new IllegalArgumentException("Unexpected backend entity sound payload length");
    }
    ByteBuf output = input.alloc().buffer(input.readableBytes() + 5);
    output.writeBytes(input, input.readerIndex(), emitterStart - input.readerIndex());
    ProtocolUtils.writeVarInt(output, clientEntityId);
    output.writeBytes(input, reader.readerIndex(), reader.readableBytes());
    return output;
  }
}
