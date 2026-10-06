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
