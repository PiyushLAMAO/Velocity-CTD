/*
 * Copyright (C) 2026 Velocity-CTD Contributors
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

package com.velocityctd.proxy.connection.fasttransition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.config.ActiveFeaturesPacket;
import com.velocitypowered.proxy.protocol.packet.config.KnownPacksPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

class ConfigStateSnapshotTest {

  private static final ProtocolVersion VERSION = ProtocolVersion.MINECRAFT_26_3;

  @Test
  void equivalentNbtKeyOrderMatches() {
    ConfigStateSnapshot first = snapshot("minecraft:dimension_type", 7,
        "minecraft:vanilla", "core", false);
    ConfigStateSnapshot reordered = snapshot("minecraft:dimension_type", 7,
        "minecraft:vanilla", "core", true);
    assertTrue(first.matches(reordered));
  }

  @Test
  void everyClientVisibleCategoryAffectsCompatibility() {
    ConfigStateSnapshot original = snapshot("minecraft:dimension_type", 7,
        "minecraft:vanilla", "core", false);
    assertFalse(original.matches(snapshot("minecraft:chat_type", 7,
        "minecraft:vanilla", "core", false)));
    assertFalse(original.matches(snapshot("minecraft:dimension_type", 8,
        "minecraft:vanilla", "core", false)));
    assertFalse(original.matches(snapshot("minecraft:dimension_type", 7,
        "minecraft:trade_rebalance", "core", false)));
    assertFalse(original.matches(snapshot("minecraft:dimension_type", 7,
        "minecraft:vanilla", "other", false)));
  }

  @Test
  void incompleteSnapshotsCannotAuthorizeFastSwitch() {
    ConfigStateSnapshot.Builder builder = ConfigStateSnapshot.builder();
    assertFalse(builder.build().matches(builder.build()));
    addRegistry(builder, "minecraft:dimension_type", false);
    builder.addTags(Map.of("minecraft:block", Map.of("minecraft:test", new int[]{7})));
    builder.addFeatures(new ActiveFeaturesPacket(new Key[]{Key.key("minecraft:vanilla")}), VERSION);
    assertFalse(builder.build().matches(builder.build()));
    builder.addKnownPacks(new KnownPacksPacket(List.of(
        new KnownPacksPacket.KnownPack("minecraft", "core", "1"))), VERSION);
    assertTrue(builder.build().matches(builder.build()));
  }

  private static ConfigStateSnapshot snapshot(String registry, int tagId, String feature,
                                              String pack, boolean reverseNbt) {
    ConfigStateSnapshot.Builder builder = ConfigStateSnapshot.builder();
    addRegistry(builder, registry, reverseNbt);
    builder.addTags(Map.of("minecraft:block", Map.of("minecraft:test", new int[]{tagId})));
    builder.addFeatures(new ActiveFeaturesPacket(new Key[]{Key.key(feature)}), VERSION);
    builder.addKnownPacks(new KnownPacksPacket(List.of(
        new KnownPacksPacket.KnownPack("minecraft", pack, "1"))), VERSION);
    return builder.build();
  }

  private static void addRegistry(ConfigStateSnapshot.Builder builder, String name,
                                  boolean reverseNbt) {
    ByteBuf payload = Unpooled.buffer();
    try {
      ProtocolUtils.writeString(payload, name);
      ProtocolUtils.writeVarInt(payload, 1);
      ProtocolUtils.writeString(payload, "minecraft:test");
      payload.writeBoolean(true);
      CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder();
      if (reverseNbt) {
        tag.putString("second", "two").putString("first", "one");
      } else {
        tag.putString("first", "one").putString("second", "two");
      }
      ProtocolUtils.writeBinaryTag(payload, VERSION, tag.build());
      builder.addRegistrySync(payload, VERSION);
    } finally {
      payload.release();
    }
  }
}
