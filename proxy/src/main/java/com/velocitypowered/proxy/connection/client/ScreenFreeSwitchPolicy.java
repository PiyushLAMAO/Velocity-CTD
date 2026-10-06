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

import com.velocitypowered.api.network.ProtocolVersion;

/** Explicitly accepted routes; matching dimensions alone never prove a safe screen-free switch. */
final class ScreenFreeSwitchPolicy {
  private ScreenFreeSwitchPolicy() {
  }

  static boolean allows(ProtocolVersion version, String previous, String destination) {
    if (version != ProtocolVersion.MINECRAFT_1_21_11
        && version != ProtocolVersion.MINECRAFT_26_1
        && version != ProtocolVersion.MINECRAFT_26_2
        && version != ProtocolVersion.MINECRAFT_26_3) {
      return false;
    }
    // Only the established Spawn route and the two accepted Economy Overworld tile owners.
    // New shard owners require protocol/gameplay acceptance before joining this allowlist.
    return (previous.equalsIgnoreCase("hub") && destination.equals("economy-route"))
        || (previous.equals("economy-route") && destination.equalsIgnoreCase("hub"))
        || (previous.equalsIgnoreCase("overworld") && isPeer(destination))
        || (isPeer(previous) && destination.equalsIgnoreCase("overworld"));
  }

  // These two owners share one Overworld weather state. Spawn still needs cleanup.
  static boolean preservesWeather(String previous, String destination) {
    return (previous.equalsIgnoreCase("overworld") && destination.equalsIgnoreCase("shard_4"))
        || (previous.equalsIgnoreCase("shard_4") && destination.equalsIgnoreCase("overworld"));
  }

  private static boolean isPeer(String name) {
    return name.equalsIgnoreCase("spawn") || name.equalsIgnoreCase("shard_4");
  }
}
