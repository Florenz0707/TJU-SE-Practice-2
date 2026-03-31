package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.elm.utils.InternalUserClient;
import cn.edu.tju.core.security.jwt.JWTFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to authenticate users. */
@RestController
@RequestMapping("/api")
@Tag(name = "管理认证", description = "提供基于JWT的身份认证和令牌管理功能")
public class AuthenticationRestController {

  private final InternalUserClient internalUserClient;

  public AuthenticationRestController(
      InternalUserClient internalUserClient) {
    this.internalUserClient = internalUserClient;
  }

  @PostMapping("/auth") /*authenticate*/
  @Operation(summary = "用户登录", description = "用户身份认证，成功后返回JWT令牌")
  public ResponseEntity<JWTToken> authorize(
      @Parameter(description = "登录信息", required = true) @Valid @RequestBody LoginDto loginDto) {

    InternalUserClient.AuthResult authResult = internalUserClient.authenticate(loginDto);
    if (authResult == null || authResult.token() == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(
        JWTFilter.AUTHORIZATION_HEADER,
        authResult.authorizationHeader() == null
            ? "Bearer " + authResult.token()
            : authResult.authorizationHeader());

    return new ResponseEntity<>(new JWTToken(authResult.token()), httpHeaders, HttpStatus.OK);
  }

  /** Object to return as body in JWT Authentication. */
  static class JWTToken {

    private String idToken;

    JWTToken(String idToken) {
      this.idToken = idToken;
    }

    @JsonProperty("id_token")
    String getIdToken() {
      return idToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }
  }
}
