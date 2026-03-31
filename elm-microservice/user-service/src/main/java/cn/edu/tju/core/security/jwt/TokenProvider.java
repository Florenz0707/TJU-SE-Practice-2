package cn.edu.tju.core.security.jwt;

import cn.edu.tju.core.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider implements InitializingBean {

  private static final String AUTHORITIES_KEY = "auth";
  private static final String USER_ID_KEY = "uid";

  private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

  private final String base64Secret;
  private final long tokenValidityInMilliseconds;
  private final long tokenValidityInMillisecondsForRememberMe;

  private Key key;

  public TokenProvider(
      @Value("${jwt.base64-secret}") String base64Secret,
      @Value("${jwt.token-validity-in-seconds}") long tokenValidityInSeconds,
      @Value("${jwt.token-validity-in-seconds-for-remember-me}")
          long tokenValidityInSecondsForRememberMe) {
    this.base64Secret = base64Secret;
    this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    this.tokenValidityInMillisecondsForRememberMe = tokenValidityInSecondsForRememberMe * 1000;
    this.afterPropertiesSet();
  }

  @Override
  public void afterPropertiesSet() {
    byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  public String createToken(Authentication authentication, boolean rememberMe) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    long now = new Date().getTime();
    Date validity =
        new Date(
            now
                + (rememberMe
                    ? this.tokenValidityInMillisecondsForRememberMe
                    : this.tokenValidityInMilliseconds));

    Long userId = extractUserId(authentication);

    JwtBuilder builder =
        Jwts.builder()
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(key, SignatureAlgorithm.HS512)
            .setExpiration(validity);

    if (userId != null) {
      builder.claim(USER_ID_KEY, userId);
    }

    return builder.compact();
  }

  public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    Collection<? extends GrantedAuthority> authorities =
        Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    Long userId = getClaimAsLong(claims, USER_ID_KEY);
    var principal = new AuthenticatedUser(userId, claims.getSubject(), "", authorities);
    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  public Long getUserId(String token) {
    return getClaimAsLong(parseClaims(token), USER_ID_KEY);
  }

  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(key).parseClaimsJws(authToken);
      return true;
    } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
      log.info("Invalid JWT signature.");
    } catch (ExpiredJwtException e) {
      log.info("Expired JWT token.");
    } catch (UnsupportedJwtException e) {
      log.info("Unsupported JWT token.");
    } catch (IllegalArgumentException e) {
      log.info("JWT token compact of handler are invalid.");
    }
    return false;
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
  }

  private Long getClaimAsLong(Claims claims, String claimName) {
    Object value = claims.get(claimName);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String stringValue) {
      try {
        return Long.valueOf(stringValue);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private Long extractUserId(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.getUserId();
    }
    return null;
  }
}