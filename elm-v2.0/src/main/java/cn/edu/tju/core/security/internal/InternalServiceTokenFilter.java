package cn.edu.tju.core.security.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Collections;

/**
 * 内部服务Token验证过滤器
 * 用于验证内部服务之间的调用
 */
public class InternalServiceTokenFilter extends GenericFilterBean {

    private static final Logger LOG = LoggerFactory.getLogger(InternalServiceTokenFilter.class);

    public static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";
    public static final String INTERNAL_SERVICE_ROLE = "INTERNAL_SERVICE";

    private final String internalServiceToken;

    public InternalServiceTokenFilter(String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String servletPath = httpServletRequest.getServletPath();

        // 只处理内部接口路径
        if (servletPath != null && servletPath.startsWith("/api/inner/")) {
            String token = httpServletRequest.getHeader(INTERNAL_SERVICE_TOKEN_HEADER);

            if (!StringUtils.hasText(token) || !token.equals(internalServiceToken)) {
                LOG.warn("Invalid or missing internal service token for URI: {}", servletPath);
                httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpServletResponse.setContentType("application/json;charset=UTF-8");
                httpServletResponse.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing internal service token\"}");
                return;
            }

            // 设置内部服务认证
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(INTERNAL_SERVICE_ROLE))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            LOG.debug("Internal service token validated for URI: {}", servletPath);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
