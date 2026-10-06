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

import java.nio.charset.StandardCharsets;
import net.kyori.adventure.key.Key;

/** Per-attempt opaque admission for the local authenticated proxy gateway. */
public final class ProxyRouteAdmission {
  public static final Key COOKIE = Key.key("insane:network_admission");
  private final byte[] ticket;

  public ProxyRouteAdmission(byte[] ticket) {
    if (ticket == null
        || ticket.length != 43
        || !new String(ticket, StandardCharsets.US_ASCII).matches("[A-Za-z0-9_-]{43}")) {
      throw new IllegalArgumentException("Invalid proxy admission");
    }
    this.ticket = ticket.clone();
  }

  public byte[] cookie(Key key) {
    return COOKIE.equals(key) ? ticket.clone() : null;
  }
}
