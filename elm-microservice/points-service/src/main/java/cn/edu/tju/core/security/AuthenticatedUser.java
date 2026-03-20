package cn.edu.tju.core.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/** Spring Security principal carrying immutable userId for downstream services. */
public class AuthenticatedUser extends org.springframework.security.core.userdetails.User {

  private final Long userId;

  public AuthenticatedUser(
      Long userId,
      String username,
      String password,
      Collection<? extends GrantedAuthority> authorities) {
    super(username, password, authorities);
    this.userId = userId;
  }

  public Long getUserId() {
    return userId;
  }
}
