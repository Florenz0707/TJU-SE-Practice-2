package cn.edu.tju;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.core.security.SecurityUtils;
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
}
