package com.velocitypowered.proxy.connection.backend;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.key.Key;
/** Per-attempt opaque admission for the local authenticated proxy gateway. */
public final class ProxyRouteAdmission {
 public static final Key COOKIE=Key.key("insane:network_admission");
 private final byte[] ticket;
 public ProxyRouteAdmission(byte[] ticket) {
  if(ticket==null || ticket.length!=43 || !new String(ticket,StandardCharsets.US_ASCII).matches("[A-Za-z0-9_-]{43}"))
   throw new IllegalArgumentException("Invalid proxy admission");
  this.ticket=ticket.clone();
 }
 public byte[] cookie(Key key) {return COOKIE.equals(key)?ticket.clone():null;}
}
