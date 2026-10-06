/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import org.junit.jupiter.api.Test;

class ScreenFreeSwitchPolicyTest {
  private static final ProtocolVersion[] SUPPORTED = {
      ProtocolVersion.MINECRAFT_1_21_11, ProtocolVersion.MINECRAFT_26_1,
      ProtocolVersion.MINECRAFT_26_2, ProtocolVersion.MINECRAFT_26_3
  };

  @Test
  void shardRoundTripAndExistingSpawnRouteWorkForEverySupportedProtocol() {
    for (ProtocolVersion version : SUPPORTED) {
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "overworld", "shard_4"));
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "shard_4", "overworld"));
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "spawn", "overworld"));
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "overworld", "spawn"));
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "SHARD_4", "OverWorld"));
    }
  }

  @Test
  void dimensionServersUnknownShardsAndOtherPairsKeepOrdinaryTransfers() {
    for (ProtocolVersion version : SUPPORTED) {
      for (String other : new String[]{"nether", "end", "shard_6", "shard_40", "overworld2"}) {
        assertFalse(ScreenFreeSwitchPolicy.allows(version, "overworld", other));
        assertFalse(ScreenFreeSwitchPolicy.allows(version, other, "overworld"));
      }
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "spawn", "shard_4"));
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "shard_4", "spawn"));
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "overworld", "overworld"));
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "shard_4", "shard_4"));
    }
  }

  @Test
  void unacceptedProtocolsNeverEnterThePacketSuppressionPath() {
    for (ProtocolVersion version : ProtocolVersion.values()) {
      boolean supported = false;
      for (ProtocolVersion accepted : SUPPORTED) {
        supported |= accepted == version;
      }
      if (!supported) {
        assertFalse(ScreenFreeSwitchPolicy.allows(version, "overworld", "shard_4"));
        assertFalse(ScreenFreeSwitchPolicy.allows(version, "shard_4", "overworld"));
        assertFalse(ScreenFreeSwitchPolicy.allows(version, "spawn", "overworld"));
      }
    }
  }

  @Test
  void preservesWeatherOnlyBetweenTheTwoSharedWeatherOwners() {
    assertTrue(com.velocitypowered.proxy.protocol.packet.GameEventPacket.EVENT_END_RAINING == 1);
    assertTrue(ScreenFreeSwitchPolicy.preservesWeather("overworld", "shard_4"));
    assertTrue(ScreenFreeSwitchPolicy.preservesWeather("SHARD_4", "OverWorld"));
    for (String other : new String[]{"spawn", "nether", "end", "shard_6", ""}) {
      assertFalse(ScreenFreeSwitchPolicy.preservesWeather("overworld", other));
      assertFalse(ScreenFreeSwitchPolicy.preservesWeather(other, "overworld"));
    }
  }

  @Test
  void acceptsOnlyTheExplicitLocalHubGatewayPair() {
    for (ProtocolVersion version : SUPPORTED) {
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "hub", "economy-route"));
      assertTrue(ScreenFreeSwitchPolicy.allows(version, "economy-route", "hub"));
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "hub", "economy"));
      assertFalse(ScreenFreeSwitchPolicy.allows(version, "hub", "nether"));
      assertFalse(ScreenFreeSwitchPolicy.preservesWeather("hub", "economy-route"));
    }
  }
}
