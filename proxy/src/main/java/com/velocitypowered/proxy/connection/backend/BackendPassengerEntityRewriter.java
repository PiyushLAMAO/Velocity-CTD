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
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;

/** Keeps the local rider's ID consistent with the retained player on Java 26.3. */
public final class BackendPassengerEntityRewriter {
  // Verified from GameProtocols.CLIENTBOUND_TEMPLATE in the primary 26.3 server codec.
  static final int SET_PASSENGERS_26_3 = 0x6D;

  private BackendPassengerEntityRewriter() { }

  /** Returns one owned replacement or null; never mutates or retains the borrowed frame. */
  public static ByteBuf rewrite(ByteBuf input, ProtocolVersion version,
      Integer backendEntityId, Integer clientEntityId) {
    if (version != ProtocolVersion.MINECRAFT_26_3 || backendEntityId == null
        || clientEntityId == null || backendEntityId.equals(clientEntityId)
        || !input.isReadable()) {
      return null;
    }
    ByteBuf reader = input.duplicate();
    if (ProtocolUtils.readVarInt(reader) != SET_PASSENGERS_26_3) {
      return null;
    }
    var replacements = new ArrayList<int[]>();
    readEntityReference(reader, backendEntityId, replacements);
    int count = ProtocolUtils.readVarInt(reader);
    if (count < 0 || count > reader.readableBytes()) {
      throw new IllegalArgumentException("Invalid backend passenger count");
    }
    for (int passenger = 0; passenger < count; passenger++) {
      readEntityReference(reader, backendEntityId, replacements);
    }
    if (reader.isReadable()) {
      throw new IllegalArgumentException("Unexpected backend passenger payload tail");
    }
    if (replacements.isEmpty()) {
      return null;
    }
    ByteBuf output = input.alloc().buffer(input.readableBytes() + 5 * replacements.size());
    try {
      int position = input.readerIndex();
      for (int[] replacement : replacements) {
        output.writeBytes(input, position, replacement[0] - position);
        ProtocolUtils.writeVarInt(output, clientEntityId);
        position = replacement[1];
      }
      output.writeBytes(input, position, input.writerIndex() - position);
      return output;
    } catch (Throwable failure) {
      output.release();
      throw failure;
    }
  }

  private static void readEntityReference(ByteBuf reader, int backendEntityId,
      ArrayList<int[]> replacements) {
    int start = reader.readerIndex();
    if (ProtocolUtils.readVarInt(reader) == backendEntityId) {
      replacements.add(new int[]{start, reader.readerIndex()});
    }
  }
}
