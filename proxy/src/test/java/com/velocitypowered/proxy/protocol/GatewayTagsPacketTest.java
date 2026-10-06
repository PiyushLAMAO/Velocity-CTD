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

package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdatePacket;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayTagsPacketTest {
  @Test
  void mapsAndRoundTripsTagsInPlay() {
    var versions =
        new ProtocolVersion[] {
          ProtocolVersion.MINECRAFT_1_21_11,
          ProtocolVersion.MINECRAFT_26_1,
          ProtocolVersion.MINECRAFT_26_2,
          ProtocolVersion.MINECRAFT_26_3
        };
    int[] ids = {132, 134, 134, 137};
    for (int i = 0; i < versions.length; i++) {
      var packet =
          new TagsUpdatePacket(
              Map.of("minecraft:dialog", Map.of("minecraft:quick_actions", new int[] {1, 3})));
      var registry =
          StateRegistry.PLAY.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND, versions[i]);
      assertEquals(ids[i], registry.getPacketId(packet));
      var data = Unpooled.buffer();
      try {
        packet.encode(data, ProtocolUtils.Direction.CLIENTBOUND, versions[i]);
        var decoded = (TagsUpdatePacket) registry.createPacket(ids[i]);
        decoded.decode(data, ProtocolUtils.Direction.CLIENTBOUND, versions[i]);
        assertArrayEquals(
            new int[] {1, 3},
            decoded.getTags().get("minecraft:dialog").get("minecraft:quick_actions"));
      } finally {
        data.release();
      }
    }
  }
}
