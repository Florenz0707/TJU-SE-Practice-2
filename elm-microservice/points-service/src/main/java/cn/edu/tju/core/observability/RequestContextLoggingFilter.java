package cn.edu.tju.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Initializes per-request MDC fields for traceability across logs and services. */
@Component
public class RequestContextLoggingFilter extends OncePerRequestFilter {

  private static final String TRACE_ID = "traceId";
  private static final String SPAN_ID = "spanId";
  private static final String REQUEST_ID = "requestId";
  private static final String USER_ID = "userId";
  private static final String ORDER_ID = "orderId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = firstNonBlank(request.getHeader("X-Trace-Id"), UUID.randomUUID().toString());
    String spanId =
        firstNonBlank(
            request.getHeader("X-Span-Id"),
            UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    String requestId = firstNonBlank(request.getHeader("X-Request-Id"), traceId);
    String orderId = firstNonBlank(request.getHeader("X-Order-Id"), "-");

    MDC.put(TRACE_ID, traceId);
    MDC.put(SPAN_ID, spanId);
    MDC.put(REQUEST_ID, requestId);
    MDC.put(USER_ID, "anonymous");
    MDC.put(ORDER_ID, orderId);

    response.setHeader("X-Trace-Id", traceId);
    response.setHeader("X-Request-Id", requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String firstNonBlank(String value, String fallback) {
    return Optional.ofNullable(value).filter(StringUtils::hasText).orElse(fallback);
  }
}
