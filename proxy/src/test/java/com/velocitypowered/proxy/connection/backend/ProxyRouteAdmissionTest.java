package com.velocitypowered.proxy.connection.backend;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
class ProxyRouteAdmissionTest {
 @Test void rejectsMalformedAdmission() {
  assertThrows(IllegalArgumentException.class,()->new ProxyRouteAdmission(null));
  assertThrows(IllegalArgumentException.class,()->new ProxyRouteAdmission(new byte[43]));
  assertThrows(IllegalArgumentException.class,()->new ProxyRouteAdmission("a".getBytes(StandardCharsets.US_ASCII)));
 }
 @Test void onlyReleasesAdmissionCookie() {
  byte[] original="A".repeat(43).getBytes(StandardCharsets.US_ASCII);
  ProxyRouteAdmission route=new ProxyRouteAdmission(original);original[0]='B';
  assertNull(route.cookie(Key.key("velocity:previous_server")));
  byte[] answer=route.cookie(ProxyRouteAdmission.COOKIE);assertEquals('A',answer[0]);
  answer[0]='C';assertEquals('A',route.cookie(ProxyRouteAdmission.COOKIE)[0]);
 }
}
