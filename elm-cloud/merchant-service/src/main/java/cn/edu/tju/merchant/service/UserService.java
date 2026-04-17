package cn.edu.tju.merchant.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import cn.edu.tju.merchant.model.User;
import cn.edu.tju.merchant.util.JwtUtils;
import cn.edu.tju.merchant.util.AuthorityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import cn.edu.tju.core.model.HttpResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String USER_SERVICE_CB = "userService";

    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;
    private final RestTemplate restTemplate;

    @Value("${internal.service.token:}")
    private String internalServiceToken;

    public UserService(JwtUtils jwtUtils, HttpServletRequest request, RestTemplate restTemplate) {
        this.jwtUtils = jwtUtils;
        this.request = request;
        this.restTemplate = restTemplate;
    }

    public User verify(String token) {
        if (token == null || token.isEmpty()) return null;
        Long uid = jwtUtils.getUserId(token);
        if (uid == null) {
            log.warn("JWT verify failed: uid missing. tokenPrefix={}", token.length() > 16 ? token.substring(0, 16) : token);
            return null;
        }

        User u = new User();
        u.setId(uid);
        u.setUsername(jwtUtils.getUsername(token));
        u.setAuthorities(AuthorityUtils.getAuthoritySet(jwtUtils.getAuthorities(token)));
        return u;
    }

    public Optional<User> getUserWithAuthorities() {
        String token = jwtUtils.resolveToken(request);
        if (token == null || token.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            String xAuthHeader = request.getHeader("X-Authorization");
            log.warn("No bearer token found in request. AuthorizationPresent={}, X-AuthorizationPresent={}",
                    authHeader != null && !authHeader.isBlank(),
                    xAuthHeader != null && !xAuthHeader.isBlank());
            return Optional.empty();
        }

        User u = verify(token);
        if (u != null) {
            return Optional.of(u);
        }
        log.warn("JWT verify returned null user. tokenPrefix={}", token.length() > 16 ? token.substring(0, 16) : token);
        return Optional.empty();
    }

    @CircuitBreaker(name = USER_SERVICE_CB, fallbackMethod = "getUserByIdFallback")
    @Retry(name = USER_SERVICE_CB)
    public Optional<User> getUserById(Long id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = jwtUtils.resolveToken(request);
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = "http://user-service/elm/api/users/" + id;

            try {
                ResponseEntity<HttpResult<User>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<HttpResult<User>>() {}
                );
                HttpResult<User> body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess()) && body.getData() != null) {
                    return Optional.of(body.getData());
                }
                if (body != null) {
                    log.warn("getUserById({}) returned non-success HttpResult: success={}, code={}, message={}", id, body.getSuccess(), body.getCode(), body.getMessage());
                } else {
                    log.warn("getUserById({}) returned empty body, status={}", id, response.getStatusCode());
                }
            } catch (Exception wrappedParseEx) {
                try {
                    ResponseEntity<User> response = restTemplate.exchange(url, HttpMethod.GET, entity, User.class);
                    if (response.getBody() != null) {
                        return Optional.of(response.getBody());
                    }
                    log.warn("getUserById({}) fallback(User) empty body, status={}", id, response.getStatusCode());
                } catch (Exception directParseEx) {
                    log.warn("getUserById({}) failed calling user-service. wrappedParseEx={}, directParseEx={}", id, wrappedParseEx.toString(), directParseEx.toString());
                }
            }
        } catch (Exception e) {
            log.warn("getUserById({}) unexpected error: {}", id, e.toString());
            throw e;
        }
        return Optional.empty();
    }

    public Optional<User> getUserByIdFallback(Long id, Exception e) {
        log.warn("Fallback triggered for getUserById({}): {}", id, e.getMessage());
        return Optional.empty();
    }

    public Optional<User> getUserWithUsername(String username) {
        User u = new User();
        if ("admin".equals(username)) {
            u.setId(1L);
            u.setUsername("admin");
        }
        return Optional.of(u);
    }

    @CircuitBreaker(name = USER_SERVICE_CB, fallbackMethod = "updateUserFallback")
    @Retry(name = USER_SERVICE_CB)
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            log.warn("Update user authorities SKIP: userId is null. user={}", user);
            return false;
        }
        if (user.getAuthorities() == null || user.getAuthorities().isEmpty()) {
            log.warn("Update user authorities SKIP: authorities empty. userId={}", user.getId());
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            boolean useInternalToken = internalServiceToken != null && !internalServiceToken.isBlank();
            if (useInternalToken) {
                headers.add("X-Internal-Service-Token", internalServiceToken);
            } else {
                String token = jwtUtils.resolveToken(request);
                if (token != null && !token.isEmpty()) {
                    headers.setBearerAuth(token);
                }
            }
            java.util.List<java.util.Map<String, String>> authList = new java.util.ArrayList<>();
            if (user.getAuthorities() != null) {
                for (String auth : user.getAuthorities()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("name", auth.startsWith("ROLE_") ? auth.substring(5) : auth);
                    authList.add(map);
                }
            }
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("authorities", authList);

            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = "http://user-service/elm/api/inner/users/" + user.getId() + "/authorities";
            log.info(
                "Updating user authorities via user-service: userId={}, url={}, authorities={}, useInternalToken={}, internalTokenEmpty={}",
                user.getId(),
                url,
                user.getAuthorities(),
                useInternalToken,
                (internalServiceToken == null || internalServiceToken.isBlank()));

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("Update user authorities FAILED: userId={}, status={}, body={}", user.getId(), resp.getStatusCode(), resp.getBody());
                return false;
            } else {
                log.info("Update user authorities OK: userId={}, status={}", user.getId(), resp.getStatusCode());
                return true;
            }
        } catch (Exception e) {
            log.warn(
                "Update user authorities ERROR: userId={}, error={}, msg={}",
                user != null ? user.getId() : null,
                e.getClass().getName(),
                e.getMessage());
            log.debug("Update user authorities ERROR stacktrace", e);
            throw e;
        }
    }

    public boolean updateUserFallback(User user, Exception e) {
        log.warn("Fallback triggered for updateUser(userId={}): {}", user != null ? user.getId() : null, e.getMessage());
        return false;
    }
}