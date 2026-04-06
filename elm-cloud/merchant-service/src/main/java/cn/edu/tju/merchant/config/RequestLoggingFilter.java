package cn.edu.tju.merchant.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  public RequestLoggingFilter() {
    log.warn("RequestLoggingFilter enabled");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long cost = System.currentTimeMillis() - start;
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String query = request.getQueryString();
      int status = response.getStatus();

      // Reduce noise from health checks / registry heartbeats.
      if (uri != null
          && (uri.contains("/actuator")
              || uri.contains("/eureka")
              || uri.contains("/favicon"))) {
        return;
      }

      String full = (query == null || query.isBlank()) ? uri : (uri + "?" + query);
      log.warn("HTTP {} {} -> {} ({}ms)", method, full, status, cost);
    }
  }
}
