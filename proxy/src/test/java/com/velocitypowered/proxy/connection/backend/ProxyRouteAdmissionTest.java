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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ProxyRouteAdmissionTest {
  @Test
  void rejectsMalformedAdmission() {
    assertThrows(IllegalArgumentException.class, () -> new ProxyRouteAdmission(null));
    assertThrows(IllegalArgumentException.class, () -> new ProxyRouteAdmission(new byte[43]));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProxyRouteAdmission("a".getBytes(StandardCharsets.US_ASCII)));
  }

  @Test
  void onlyReleasesAdmissionCookie() {
    byte[] original = "A".repeat(43).getBytes(StandardCharsets.US_ASCII);
    ProxyRouteAdmission route = new ProxyRouteAdmission(original);
    original[0] = 'B';
    assertNull(route.cookie(Key.key("velocity:previous_server")));
    byte[] answer = route.cookie(ProxyRouteAdmission.COOKIE);
    assertEquals('A', answer[0]);
    answer[0] = 'C';
    assertEquals('A', route.cookie(ProxyRouteAdmission.COOKIE)[0]);
  }
}
