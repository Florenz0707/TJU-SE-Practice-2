package cn.edu.tju.core.security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.elm.utils.InternalUserClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationRestControllerTest {

  @Mock private InternalUserClient internalUserClient;

  @InjectMocks private AuthenticationRestController authenticationRestController;

  @Test
  void authorize_shouldReturnUnauthorizedWhenAuthenticationFails() {
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("user");
    loginDto.setPassword("123456");
    when(internalUserClient.authenticate(loginDto)).thenReturn(null);

    var response = authenticationRestController.authorize(loginDto);

    assertEquals(401, response.getStatusCode().value());
    assertNull(response.getBody());
  }

  @Test
  void authorize_shouldBuildBearerHeaderWhenDownstreamHeaderMissing() {
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("user");
    loginDto.setPassword("123456");
    when(internalUserClient.authenticate(loginDto))
        .thenReturn(new InternalUserClient.AuthResult("jwt-token", null));

    var response = authenticationRestController.authorize(loginDto);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Bearer jwt-token", response.getHeaders().getFirst(JWTFilter.AUTHORIZATION_HEADER));
    assertNotNull(response.getBody());
    assertEquals("jwt-token", response.getBody().getIdToken());
  }

  @Test
  void authorize_shouldPreserveDownstreamAuthorizationHeader() {
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("admin");
    loginDto.setPassword("123456");
    when(internalUserClient.authenticate(loginDto))
        .thenReturn(new InternalUserClient.AuthResult("jwt-token", "Bearer upstream-token"));

    var response = authenticationRestController.authorize(loginDto);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Bearer upstream-token", response.getHeaders().getFirst(AUTHORIZATION));
    assertNotNull(response.getBody());
    assertEquals("jwt-token", response.getBody().getIdToken());
  }
}