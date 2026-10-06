package net.insanesmp.deploy;

import java.nio.file.Files;
import java.nio.file.Path;

/** Retains the existing independent Main and Economy Redis namespaces. */
public final class ProxyBootstrap {
  public static void main(String[] args) throws Exception {
    String namespace = Files.readString(Path.of("redis-namespace.txt")).trim();
    if (!namespace.matches("[a-z0-9-]{1,32}")) {
      throw new IllegalArgumentException("Invalid Redis namespace");
    }
    System.setProperty("velocity.redis-namespace", namespace);
    System.out.println("Insane proxy Redis namespace: " + namespace);
    Class.forName("com.velocitypowered.proxy.Velocity")
        .getMethod("main", String[].class).invoke(null, (Object) args);
  }
}
