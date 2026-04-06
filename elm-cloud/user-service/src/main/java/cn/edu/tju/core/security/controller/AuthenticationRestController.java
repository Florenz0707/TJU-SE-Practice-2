package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.core.security.jwt.TokenProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Controller to authenticate users. */
@RestController
@RequestMapping("/api")
@Tag(name = "管理认证", description = "提供基于JWT的身份认证和令牌管理功能")
public class AuthenticationRestController {

  private final TokenProvider tokenProvider;

  private final AuthenticationManagerBuilder authenticationManagerBuilder;

  public AuthenticationRestController(
      TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
    this.tokenProvider = tokenProvider;
    this.authenticationManagerBuilder = authenticationManagerBuilder;
  }

  @PostMapping("/auth") /*authenticate*/
  @Operation(summary = "用户登录", description = "用户身份认证，成功后返回JWT令牌")
  public ResponseEntity<JWTToken> authorize(
      @Parameter(description = "登录信息", required = true) @Valid @RequestBody LoginDto loginDto) {

    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

    Authentication authentication =
        authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    boolean rememberMe = (loginDto.isRememberMe() != null && loginDto.isRememberMe());
    String jwt = tokenProvider.createToken(authentication, rememberMe);

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);

    return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
  }

  @PostMapping("/auth/refresh")
  @Operation(summary = "刷新JWT令牌", description = "使用refresh_token刷新并返回新的JWT令牌（教学/演示用实现）")
  public ResponseEntity<JWTToken> refreshToken(
      @Parameter(description = "刷新令牌", required = true) @RequestBody Map<String, Object> body) {
    Object refresh = body == null ? null : body.get("refresh_token");
    if (refresh == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // NOTE: 当前项目的 TokenProvider 主要生成 access token。
    // 为了与前端的 refresh 流程对齐，这里采用“复用 refresh_token 作为 rememberMe=true 的再签发触发器”的简化实现。
    // 如果后续要做严谨的 refresh token（持久化、过期、撤销），建议单独建表存储 refresh token。
    boolean rememberMe = true;
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      // 没有登录上下文就无法安全地刷新；让前端走重新登录。
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    String jwt = tokenProvider.createToken(authentication, rememberMe);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);

    // 兼容前端类型：同时返回新的 id_token，并原样返回 refresh_token。
    return new ResponseEntity<>(new JWTToken(jwt, refresh.toString()), httpHeaders, HttpStatus.OK);
  }

  /** Object to return as body in JWT Authentication. */
  static class JWTToken {

    private String idToken;

    private String refreshToken;

    JWTToken(String idToken) {
      this.idToken = idToken;
    }

    JWTToken(String idToken, String refreshToken) {
      this.idToken = idToken;
      this.refreshToken = refreshToken;
    }

    @JsonProperty("id_token")
    String getIdToken() {
      return idToken;
    }

    @JsonProperty("refresh_token")
    String getRefreshToken() {
      return refreshToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }

    void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }
  }
}
