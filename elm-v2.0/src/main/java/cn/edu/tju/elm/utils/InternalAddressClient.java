package cn.edu.tju.elm.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InternalAddressClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalAddressClient(
      RestTemplate restTemplate,
      @Value("${address.service.url:http://localhost:8085/elm}") String addressServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = restTemplate;
    this.baseUrl =
        addressServiceUrl.endsWith("/")
            ? addressServiceUrl.substring(0, addressServiceUrl.length() - 1)
            : addressServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    return headers;
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

  private Map<?, ?> getInternal(String path) {
    String url = baseUrl + path;
    HttpEntity<Void> request = new HttpEntity<>(createHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    return response.getBody();
  }

  private static Long readLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private static Integer readInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Integer integerValue) {
      return integerValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  public AddressSnapshot createAddress(CreateAddressCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customerId", command.customerId());
    requestBody.put("contactName", command.contactName());
    requestBody.put("contactSex", command.contactSex());
    requestBody.put("contactTel", command.contactTel());
    requestBody.put("address", command.address());
    try {
      String url = baseUrl + "/api/inner/address";
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      return toAddressSnapshot(readMapData(response.getBody()));
    } catch (Exception e) {
      System.err.println("Failed to create address: " + e.getMessage());
      return null;
    }
  }

  public AddressSnapshot getAddressById(Long addressId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/address/" + addressId);
      return toAddressSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get address by id: " + e.getMessage());
      return null;
    }
  }

  public List<AddressSnapshot> getAddressesByCustomerId(Long customerId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/address/customer/" + customerId);
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
          .map(this::toAddressSnapshot)
          .filter(snapshot -> snapshot != null)
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to get addresses by customer id: " + e.getMessage());
      return List.of();
    }
  }

  public AddressSnapshot updateAddress(Long addressId, UpdateAddressCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customerId", command.customerId());
    requestBody.put("contactName", command.contactName());
    requestBody.put("contactSex", command.contactSex());
    requestBody.put("contactTel", command.contactTel());
    requestBody.put("address", command.address());
    try {
      String url = baseUrl + "/api/inner/address/" + addressId;
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
      return toAddressSnapshot(readMapData(response.getBody()));
    } catch (Exception e) {
      System.err.println("Failed to update address: " + e.getMessage());
      return null;
    }
  }

  public boolean deleteAddress(Long addressId) {
    try {
      String url = baseUrl + "/api/inner/address/" + addressId;
      HttpEntity<Void> request = new HttpEntity<>(createHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
      Map<?, ?> body = response.getBody();
      if (!isSuccessResponse(body)) {
        return false;
      }
      Object data = body == null ? null : body.get("data");
      return data instanceof Boolean value && value;
    } catch (Exception e) {
      System.err.println("Failed to delete address: " + e.getMessage());
      return false;
    }
  }

  private AddressSnapshot toAddressSnapshot(Map<?, ?> data) {
    if (data == null) {
      return null;
    }
    return new AddressSnapshot(
        readLong(data.get("id")),
        readLong(data.get("customerId")),
        data.get("contactName") == null ? null : String.valueOf(data.get("contactName")),
        readInteger(data.get("contactSex")),
        data.get("contactTel") == null ? null : String.valueOf(data.get("contactTel")),
        data.get("address") == null ? null : String.valueOf(data.get("address")));
  }

  public record AddressSnapshot(
      Long id,
      Long customerId,
      String contactName,
      Integer contactSex,
      String contactTel,
      String address) {}

  public record CreateAddressCommand(
      Long customerId, String contactName, Integer contactSex, String contactTel, String address) {}

  public record UpdateAddressCommand(
      Long customerId, String contactName, Integer contactSex, String contactTel, String address) {}
}