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

import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProxyRouteTargetsTest {
  @Test
  void permitsOnlyConfiguredRemoteHostAndPort() {
    List<String> targets = List.of("167.104.101.158:25212");
    assertTrue(ProxyRouteTargets.isAllowed(new InetSocketAddress("167.104.101.158",25212),targets));
    assertFalse(ProxyRouteTargets.isAllowed(new InetSocketAddress("167.104.101.158",25213),targets));
    assertFalse(ProxyRouteTargets.isAllowed(new InetSocketAddress("167.104.101.146",25212),targets));
    assertFalse(ProxyRouteTargets.isAllowed(InetSocketAddress.createUnresolved("167.104.101.158",25212),targets));
    assertTrue(ProxyRouteTargets.isAllowed(new InetSocketAddress("127.0.0.1",25690),List.of()));
  }
}
