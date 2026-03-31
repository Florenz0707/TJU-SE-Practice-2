package cn.edu.tju.core.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.edu.tju.core.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class TokenProviderTest {

  private static final String SECRET =
      "ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=";

  @Test
  void createValidateAndParseTokenRoundTrip() {
    TokenProvider tokenProvider = new TokenProvider(SECRET, 86400, 108000);
    AuthenticatedUser principal =
        new AuthenticatedUser(
            42L,
            "alice",
            "",
            List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("USER")));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());

    String token = tokenProvider.createToken(authentication, true);

    assertNotNull(token);
    assertTrue(tokenProvider.validateToken(token));
    assertEquals(42L, tokenProvider.getUserId(token));

    Authentication parsed = tokenProvider.getAuthentication(token);
    assertEquals("alice", parsed.getName());
    assertEquals(2, parsed.getAuthorities().size());
    assertTrue(
        parsed.getAuthorities().stream()
            .anyMatch(authority -> "ADMIN".equals(authority.getAuthority())));
  }
}