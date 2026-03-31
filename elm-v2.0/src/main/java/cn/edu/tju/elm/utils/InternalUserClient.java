package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InternalUserClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalUserClient(
      RestTemplate restTemplate,
      @Value("${user.service.url:http://localhost:8086/elm}") String userServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = restTemplate;
    this.baseUrl =
        userServiceUrl.endsWith("/")
            ? userServiceUrl.substring(0, userServiceUrl.length() - 1)
            : userServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  public AuthResult authenticate(LoginDto loginDto) {
    try {
      String url = baseUrl + "/api/auth";
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<LoginDto> request = new HttpEntity<>(loginDto, headers);
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      Map<?, ?> body = response.getBody();
      if (body == null) {
        return null;
      }
      Object token = body.get("id_token");
      if (token == null) {
        return null;
      }
      return new AuthResult(String.valueOf(token), response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    } catch (Exception e) {
      System.err.println("Failed to authenticate via user-service: " + e.getMessage());
      return null;
    }
  }

  public User getUserById(Long userId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/users/" + userId);
      return toUser(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get user by id: " + e.getMessage());
      return null;
    }
  }

  public User getUserByUsername(String username) {
    try {
      Map<?, ?> body = getInternal("/api/inner/users/by-username/" + username);
      return toUser(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get user by username: " + e.getMessage());
      return null;
    }
  }

  public List<User> getUsers() {
    try {
      Map<?, ?> body = getInternal("/api/inner/users");
      if (!isSuccessResponse(body)) {
        return List.of();
      }
      Object data = body.get("data");
      if (!(data instanceof List<?> list)) {
        return List.of();
      }
      return list.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(this::toUser)
          .filter(user -> user != null)
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to list users: " + e.getMessage());
      return List.of();
    }
  }

  public User createUser(User user) {
    try {
      Map<?, ?> body = postInternal("/api/inner/users", toInternalUserPayload(user));
      return toUser(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to create user: " + e.getMessage());
      return null;
    }
  }

  public Person createPerson(Person person) {
    try {
      Map<?, ?> body = postInternal("/api/inner/persons", toInternalUserPayload(person));
      User user = toUser(readMapData(body));
      return user instanceof Person result ? result : null;
    } catch (Exception e) {
      System.err.println("Failed to create person: " + e.getMessage());
      return null;
    }
  }

  public User updateUser(User user) {
    try {
      Map<?, ?> body = putInternal("/api/inner/users/" + user.getId(), toInternalUserPayload(user));
      return toUser(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to update user: " + e.getMessage());
      return null;
    }
  }

  private HttpHeaders createInternalHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    return headers;
  }

  private Map<?, ?> getInternal(String path) {
    HttpEntity<Void> request = new HttpEntity<>(createInternalHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.GET, request, Map.class);
    return response.getBody();
  }

  private Map<?, ?> postInternal(String path, Object requestBody) {
    HttpEntity<Object> request = new HttpEntity<>(requestBody, createInternalHeaders());
    ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + path, request, Map.class);
    return response.getBody();
  }

  private Map<?, ?> putInternal(String path, Object requestBody) {
    HttpEntity<Object> request = new HttpEntity<>(requestBody, createInternalHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.PUT, request, Map.class);
    return response.getBody();
  }

  private Map<String, Object> toInternalUserPayload(User user) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("id", user.getId());
    payload.put("username", user.getUsername());
    payload.put("password", user.getPassword());
    payload.put("activated", user.isActivated());
    payload.put("deleted", user.getDeleted());
    payload.put("createTime", user.getCreateTime());
    payload.put("updateTime", user.getUpdateTime());
    payload.put("authorities", user.getAuthorities());
    if (user instanceof Person person) {
      payload.put("firstName", person.getFirstName());
      payload.put("lastName", person.getLastName());
      payload.put("email", person.getEmail());
      payload.put("phone", person.getPhone());
      payload.put("gender", person.getGender());
      payload.put("photo", person.getPhoto());
    }
    return payload;
  }

  private boolean isSuccessResponse(Map<?, ?> body) {
    return body != null && Boolean.TRUE.equals(body.get("success"));
  }

  private Map<?, ?> readMapData(Map<?, ?> body) {
    if (!isSuccessResponse(body)) {
      return null;
    }
    Object data = body.get("data");
    if (data instanceof Map<?, ?> map) {
      return map;
    }
    return null;
  }

  private User toUser(Map<?, ?> data) {
    if (data == null) {
      return null;
    }
    User user = hasPersonFields(data) ? new Person() : new User();
    user.setId(readLong(data.get("id")));
    user.setUsername(readString(data.get("username")));
    user.setPassword(readString(data.get("password")));
    user.setActivated(Boolean.TRUE.equals(data.get("activated")));
    user.setDeleted(readBoolean(data.get("deleted")));
    user.setCreateTime(readDateTime(data.get("createTime")));
    user.setUpdateTime(readDateTime(data.get("updateTime")));
    user.setAuthorities(readAuthorities(data.get("authorities")));
    if (user instanceof Person person) {
      person.setFirstName(readString(data.get("firstName")));
      person.setLastName(readString(data.get("lastName")));
      person.setEmail(readString(data.get("email")));
      person.setPhone(readString(data.get("phone")));
      person.setGender(readString(data.get("gender")));
      person.setPhoto(readString(data.get("photo")));
    }
    return user;
  }

  private boolean hasPersonFields(Map<?, ?> data) {
    return data.containsKey("firstName")
        || data.containsKey("lastName")
        || data.containsKey("email")
        || data.containsKey("phone")
        || data.containsKey("gender")
        || data.containsKey("photo");
  }

  private Set<Authority> readAuthorities(Object value) {
    Set<Authority> authorities = new HashSet<>();
    if (!(value instanceof List<?> list)) {
      return authorities;
    }
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        String name = readString(map.get("name"));
        if (name != null) {
          Authority authority = new Authority();
          authority.setName(name);
          authorities.add(authority);
        }
      }
    }
    return authorities;
  }

  private Long readLong(Object value) {
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return value == null ? null : Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private Boolean readBoolean(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    return value == null ? null : Boolean.parseBoolean(String.valueOf(value));
  }

  private String readString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private LocalDateTime readDateTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(String.valueOf(value));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public record AuthResult(String token, String authorizationHeader) {}
}
