package cn.edu.tju.core.security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.core.security.jwt.TokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthenticationRestControllerTest {

  @Mock private TokenProvider tokenProvider;
  @Mock private AuthenticationManagerBuilder authenticationManagerBuilder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private Authentication authentication;

  @InjectMocks private AuthenticationRestController authenticationRestController;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authorizeReturnsJwtHeaderAndBody() {
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("alice");
    loginDto.setPassword("123456");
    loginDto.setRememberMe(true);

    when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(tokenProvider.createToken(eq(authentication), eq(true))).thenReturn("jwt-token");

    ResponseEntity<?> response = authenticationRestController.authorize(loginDto);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Bearer jwt-token", response.getHeaders().getFirst(JWTFilter.AUTHORIZATION_HEADER));
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    assertEquals("jwt-token", ((AuthenticationRestController.JWTToken) response.getBody()).getIdToken());
  }
}