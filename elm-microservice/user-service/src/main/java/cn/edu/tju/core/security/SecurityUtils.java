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

  public static Optional<String> getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      LOG.debug("no authentication in security context found");
      return Optional.empty();
    }

    String username = null;
    if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
      username = springSecurityUser.getUsername();
    } else if (authentication.getPrincipal() instanceof String value) {
      username = value;
    }

    return Optional.ofNullable(username);
  }

  public static Optional<Long> getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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

  public static String bCryptPasswordEncode(String password) {
    return new BCryptPasswordEncoder().encode(password);
  }
}