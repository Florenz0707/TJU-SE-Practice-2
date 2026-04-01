package cn.edu.tju.core.security;

import java.io.Serial;
import org.springframework.security.core.AuthenticationException;

/** This exception is thrown in case of a not activated user trying to authenticate. */
public class UserNotActivatedException extends AuthenticationException {

  @Serial private static final long serialVersionUID = -1126699074574529145L;

  public UserNotActivatedException(String message) {
    super(message);
  }
}
