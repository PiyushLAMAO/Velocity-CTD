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

import static org.junit.jupiter.api.Assertions.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.EntityIdPayloadPacket;
import com.velocitypowered.proxy.protocol.packet.EntityMetadataPacket;
import com.velocitypowered.proxy.protocol.packet.GameEventPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeferredPlayPacketsTest {
  @Test
  void rawWorldPacketsAndTypedPlayerStateReplayInOrderThroughPlayDispatch() {
    var held = new DeferredPlayPackets();
    var entity = new EntityMetadataPacket();
    entity.setEntityId(27);
    ByteBuf world = Unpooled.buffer().writeInt(123);
    held.add(entity);
    held.add(world);
    held.add(new GameEventPacket(13, 0));
    world.release(); // MinecraftConnection releases the original network-read ownership.
    List<String> seen = new ArrayList<>();
    held.replay(new MinecraftSessionHandler() {
      public boolean handle(EntityIdPayloadPacket packet) {
        packet.setEntityId(Integer.MAX_VALUE);
        seen.add("rewrite");
        return false;
      }
      public boolean handle(GameEventPacket packet) {
        seen.add("suppressed-loading");
        return true;
      }
      public void handleGeneric(MinecraftPacket packet) {
        seen.add("forward:" + ((EntityIdPayloadPacket) packet).getEntityId());
      }
      public void handleUnknown(ByteBuf buf) {
        seen.add("chunk:" + buf.readInt());
      }
      public void readCompleted() {
        seen.add("flush");
      }
    });
    assertEquals(List.of("rewrite", "forward:2147483647", "chunk:123", "suppressed-loading", "flush"), seen);
    assertEquals(0, world.refCnt());
  }

  @Test
  void abandonedConnectionReleasesEveryHeldBuffer() {
    var held = new DeferredPlayPackets();
    ByteBuf world = Unpooled.buffer().writeByte(1);
    held.add(world);
    world.release();
    held.clear();
    held.clear();
    assertEquals(0, world.refCnt());
  }

  @Test
  void dispatchFailureReleasesTheCurrentPacketAndRemainingTail() {
    var held = new DeferredPlayPackets();
    ByteBuf first = Unpooled.buffer().writeByte(1);
    ByteBuf second = Unpooled.buffer().writeByte(2);
    held.add(first); held.add(second);
    first.release(); second.release();
    assertThrows(IllegalStateException.class, () -> held.replay(new MinecraftSessionHandler() {
      public void handleUnknown(ByteBuf packet) {
        throw new IllegalStateException("closed");
      }
    }));
    assertEquals(0, first.refCnt());
    assertEquals(0, second.refCnt());
  }
}
