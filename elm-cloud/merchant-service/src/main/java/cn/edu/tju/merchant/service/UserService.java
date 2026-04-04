package cn.edu.tju.merchant.service;

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

@Service
public class UserService {

    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;
    private final RestTemplate restTemplate;

    public UserService(JwtUtils jwtUtils, HttpServletRequest request, RestTemplate restTemplate) {
        this.jwtUtils = jwtUtils;
        this.request = request;
        this.restTemplate = restTemplate;
    }

    public User verify(String token) {
        if (token == null || token.isEmpty()) return null;
        Long uid = jwtUtils.getUserId(token);
        if (uid == null) return null;

        User u = new User();
        u.setId(uid);
        u.setUsername(jwtUtils.getUsername(token));
        u.setAuthorities(AuthorityUtils.getAuthoritySet(jwtUtils.getAuthorities(token)));
        return u;
    }

    public Optional<User> getUserWithAuthorities() {
        String token = jwtUtils.resolveToken(request);
        User u = verify(token);
        if (u != null) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public Optional<User> getUserById(Long id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = jwtUtils.resolveToken(request);
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<HttpResult<User>> response = restTemplate.exchange(
                "http://user-service/elm/api/users/" + id,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<HttpResult<User>>() {}
            );
            if (response.getBody() != null && response.getBody().getSuccess() && response.getBody().getData() != null) {
                return Optional.of(response.getBody().getData());
            }
        } catch (Exception e) {
            // Ignore or log error
        }
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
    public void updateUser(User user) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = jwtUtils.resolveToken(request);
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            // convert Set<String> to Set<Map<String, String>>
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
            restTemplate.exchange(
                "http://user-service/elm/api/users/" + user.getId() + "/authorities",
                HttpMethod.PATCH,
                entity,
                Void.class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}