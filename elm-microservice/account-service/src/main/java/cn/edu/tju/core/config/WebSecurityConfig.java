package cn.edu.tju.core.config;

import cn.edu.tju.core.observability.RequestContextLoggingFilter;
import cn.edu.tju.core.security.internal.InternalServiceTokenFilter;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.core.security.jwt.TokenProvider;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  private final TokenProvider tokenProvider;
  private final RequestContextLoggingFilter requestContextLoggingFilter;

  public WebSecurityConfig(
      TokenProvider tokenProvider, RequestContextLoggingFilter requestContextLoggingFilter) {
    this.tokenProvider = tokenProvider;
    this.requestContextLoggingFilter = requestContextLoggingFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 跨域配置 This bean is picked up by .cors(Customizer.withDefaults()) */
  @Bean
  // <-- IMPORTANT: Expose this as a Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost",
            "http://127.0.0.1",
            "http://localhost:4173",
            "http://127.0.0.1:4173"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Collections.singletonList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private final String[] permitUrlArr =
      new String[] {
        "/hello",
        "/api/auth",
        "/api/persons",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/h2-console/**",
        "/**.jsp",
        "/**.html"
      };

  @Value("${internal.service.token}")
  private String internalServiceToken;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    var jwtTokenFilter = new JWTFilter(tokenProvider);
    var internalServiceTokenFilter = new InternalServiceTokenFilter(internalServiceToken);

    httpSecurity.removeConfigurers(DefaultLoginPageConfigurer.class);

    return httpSecurity
        // Enable CORS using the corsConfigurationSource bean
        .cors(Customizer.withDefaults())
        // Disable CSRF since you are using JWT
        .csrf(AbstractHttpConfigurer::disable)
        // Authorize HTTP requests
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(permitUrlArr)
                    .permitAll()
                    // 内部接口需要内部服务Token或JWT认证
                    .requestMatchers("/api/inner/**")
                    .hasAnyAuthority("INTERNAL_SERVICE", "ROLE_USER", "ROLE_ADMIN")
                    .anyRequest()
                    .authenticated())
        // Set session management to stateless
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Disable unnecessary headers
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        // Build request context first for all subsequent logs.
        .addFilterBefore(requestContextLoggingFilter, UsernamePasswordAuthenticationFilter.class)
        // Authenticate internal calls before generic JWT handling.
        .addFilterAfter(internalServiceTokenFilter, RequestContextLoggingFilter.class)
        // Install JWT-based user context after internal token check.
        .addFilterAfter(jwtTokenFilter, InternalServiceTokenFilter.class)
        // Disable default login mechanisms
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .rememberMe(AbstractHttpConfigurer::disable)
        .build();
  }
}
