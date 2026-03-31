package cn.edu.tju.core.security.jwt;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JWTFilterTest {

  @Mock private TokenProvider tokenProvider;
  @Mock private Authentication authentication;
  @Mock private FilterChain filterChain;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterSetsAuthenticationForValidBearerToken() throws Exception {
    JWTFilter filter = new JWTFilter(tokenProvider);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(tokenProvider.validateToken("valid-token")).thenReturn(true);
    when(tokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

    filter.doFilter(request, response, filterChain);

    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterIgnoresMissingOrInvalidToken() throws Exception {
    JWTFilter filter = new JWTFilter(tokenProvider);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Token invalid-format");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}