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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** An explicit numeric address and port allowlist for authenticated proxy routes. */
final class ProxyRouteTargets {
  private static final List<String> TARGETS = load();

  private ProxyRouteTargets() { }

  static boolean isAllowed(InetSocketAddress address) {
    return isAllowed(address, TARGETS);
  }

  static boolean isAllowed(InetSocketAddress address, List<String> targets) {
    if (address.getAddress() == null) {
      return false;
    }
    if (address.getAddress().isLoopbackAddress()) {
      return true;
    }
    String exact = address.getAddress().getHostAddress() + ":" + address.getPort();
    return targets.contains(exact);
  }

  private static List<String> load() {
    Path file = Path.of("proxy-route-targets.txt");
    try {
      if (!Files.isRegularFile(file) || Files.size(file) > 4096) {
        return List.of();
      }
      List<String> targets = Files.readAllLines(file).stream().map(String::trim)
          .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
      if (targets.size() > 16 || targets.stream()
          .anyMatch(line -> !line.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}:[0-9]{1,5}"))) {
        return List.of();
      }
      return targets;
    } catch (IOException exception) {
      return List.of();
    }
  }
}
