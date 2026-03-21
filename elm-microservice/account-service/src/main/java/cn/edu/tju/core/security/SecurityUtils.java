package cn.edu.tju.core.security;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

  private SecurityUtils() {}

  /**
   * Get the login of the current user.
   *
   * @return the login of the current user.
   */
  public static Optional<String> getCurrentUsername() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      LOG.debug("no authentication in security context found");
      return Optional.empty();
    }

    String username = null;
    if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
      username = springSecurityUser.getUsername();
    } else if (authentication.getPrincipal() instanceof String) {
      username = (String) authentication.getPrincipal();
    }

    LOG.debug("found username '{}' in security context", username);

    return Optional.ofNullable(username);
  }

  public static Optional<Long> getCurrentUserId() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      LOG.debug("no authentication in security context found");
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser authenticatedUser) {
      return Optional.ofNullable(authenticatedUser.getUserId());
    }
    return Optional.empty();
  }

  public static String BCryptPasswordEncode(String pass) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder.encode(pass);
  }
}
