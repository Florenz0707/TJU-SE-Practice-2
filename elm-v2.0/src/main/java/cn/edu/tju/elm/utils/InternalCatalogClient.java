package cn.edu.tju.elm.utils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
public class InternalCatalogClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalCatalogClient(
      @Value("${catalog.service.url:http://localhost:8080/elm}") String catalogServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = new RestTemplate();
    this.baseUrl =
        catalogServiceUrl.endsWith("/")
            ? catalogServiceUrl.substring(0, catalogServiceUrl.length() - 1)
            : catalogServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    return headers;
  }

  private Map<?, ?> getInternal(String path) {
    String url = baseUrl + path;
    HttpEntity<Void> request = new HttpEntity<>(createHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    return response.getBody();
  }

  private Map<?, ?> postInternal(String path, Map<String, Object> requestBody) {
    String url = baseUrl + path;
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    return response.getBody();
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

  private static BigDecimal readBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private static Boolean readBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static LocalTime readLocalTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalTime.parse(String.valueOf(value));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public BusinessSnapshot getBusinessSnapshot(Long businessId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/catalog/business/" + businessId);
      Map<?, ?> data = readMapData(body);
      if (data == null) {
        return null;
      }
      return new BusinessSnapshot(
          readLong(data.get("id")),
          readBoolean(data.get("deleted")),
          readBigDecimal(data.get("startPrice")),
          readBigDecimal(data.get("deliveryPrice")),
          readLocalTime(data.get("openTime")),
          readLocalTime(data.get("closeTime")));
    } catch (Exception e) {
      System.err.println("Failed to get business snapshot: " + e.getMessage());
      return null;
    }
  }

  public FoodSnapshot getFoodSnapshot(Long foodId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/catalog/food/" + foodId);
      Map<?, ?> data = readMapData(body);
      if (data == null) {
        return null;
      }
      return new FoodSnapshot(
          readLong(data.get("id")),
          readLong(data.get("businessId")),
          readBoolean(data.get("deleted")),
          readBigDecimal(data.get("foodPrice")),
          readInteger(data.get("stock")));
    } catch (Exception e) {
      System.err.println("Failed to get food snapshot: " + e.getMessage());
      return null;
    }
  }

  public boolean reserveStock(String requestId, String orderId, List<StockItem> items) {
    return adjustStock("/api/inner/catalog/stock/reserve", requestId, orderId, items);
  }

  public boolean releaseStock(String requestId, String orderId, List<StockItem> items) {
    return adjustStock("/api/inner/catalog/stock/release", requestId, orderId, items);
  }

  private boolean adjustStock(
      String path, String requestId, String orderId, List<StockItem> items) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", requestId);
    requestBody.put("orderId", orderId);
    requestBody.put("items", items);
    try {
      Map<?, ?> responseBody = postInternal(path, requestBody);
      if (!isSuccessResponse(responseBody)) {
        return false;
      }
      Object data = responseBody.get("data");
      return Boolean.TRUE.equals(readBoolean(data));
    } catch (Exception e) {
      System.err.println("Failed to adjust stock: " + e.getMessage());
      return false;
    }
  }

  public record BusinessSnapshot(
      Long businessId,
      Boolean deleted,
      BigDecimal startPrice,
      BigDecimal deliveryPrice,
      LocalTime openTime,
      LocalTime closeTime) {}

  public record FoodSnapshot(
      Long foodId, Long businessId, Boolean deleted, BigDecimal foodPrice, Integer stock) {}

  public record StockItem(Long foodId, Integer quantity) {}
}
