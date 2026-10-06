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

/** Isolates the retained local player from ordinary positive backend entity counters. */
public final class ClientEntityIds {
  public static final int SELF = Integer.MAX_VALUE;

  private ClientEntityIds() { }

  public static int forJoin(ProtocolVersion version, int backendId) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_11) ? SELF : backendId;
  }
}
