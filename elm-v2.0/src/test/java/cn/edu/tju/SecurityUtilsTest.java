package cn.edu.tju;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.core.security.AuthenticatedUser;
import cn.edu.tju.core.security.SecurityUtils;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilsTest {

  @Test
  public void getCurrentUsername() {
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));
    SecurityContextHolder.setContext(securityContext);

    Optional<String> username = SecurityUtils.getCurrentUsername();

    assertThat(username).contains("admin");
  }

  @Test
  public void getCurrentUsernameForNoAuthenticationInContext() {
    Optional<String> username = SecurityUtils.getCurrentUsername();

    assertThat(username).isEmpty();
  }

  @Test
  public void getCurrentUserId() {
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    var principal = new AuthenticatedUser(1001L, "admin", "N/A", Collections.emptyList());
    securityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    SecurityContextHolder.setContext(securityContext);

    Optional<Long> userId = SecurityUtils.getCurrentUserId();

    assertThat(userId).contains(1001L);
  }

  @Test
  public void getCurrentUserIdForNoAuthenticationInContext() {
    Optional<Long> userId = SecurityUtils.getCurrentUserId();

    assertThat(userId).isEmpty();
  }
}
