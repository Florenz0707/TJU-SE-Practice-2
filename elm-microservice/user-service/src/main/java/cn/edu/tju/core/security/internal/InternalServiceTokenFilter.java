package cn.edu.tju.core.security.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

public class InternalServiceTokenFilter extends GenericFilterBean {

  public static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";
  public static final String INTERNAL_SERVICE_ROLE = "INTERNAL_SERVICE";

  private final String internalServiceToken;

  public InternalServiceTokenFilter(String internalServiceToken) {
    this.internalServiceToken = internalServiceToken;
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String servletPath = request.getServletPath();

    if (servletPath != null && servletPath.startsWith("/api/inner/")) {
      String token = request.getHeader(INTERNAL_SERVICE_TOKEN_HEADER);
      if (!StringUtils.hasText(token) || !token.equals(internalServiceToken)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response
            .getWriter()
            .write(
                "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing internal service token\"}");
        return;
      }

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(
              "internal-service",
              null,
              Collections.singletonList(new SimpleGrantedAuthority(INTERNAL_SERVICE_ROLE)));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }
}