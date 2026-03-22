package cn.edu.tju.elm.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class InternalOrderClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalOrderClient(
      @Value("${order.service.url:http://localhost:8080/elm}") String orderServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = new RestTemplate();
    this.baseUrl =
        orderServiceUrl.endsWith("/")
            ? orderServiceUrl.substring(0, orderServiceUrl.length() - 1)
            : orderServiceUrl;
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

  private static LocalDateTime readDateTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(String.valueOf(value));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public OrderSnapshot getOrderById(Long orderId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/" + orderId);
      return toSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get order by id: " + e.getMessage());
      return null;
    }
  }

  public OrderSnapshot getOrderByRequestId(String requestId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/by-request/" + requestId);
      return toSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get order by request id: " + e.getMessage());
      return null;
    }
  }

  public List<OrderSnapshot> getOrdersByCustomerId(Long customerId) {
    return getOrderList("/api/inner/order/customer/" + customerId);
  }

  public List<OrderSnapshot> getOrdersByBusinessId(Long businessId) {
    return getOrderList("/api/inner/order/business/" + businessId);
  }

  public List<OrderDetailSnapshot> getOrderDetailsByOrderId(Long orderId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/" + orderId + "/details");
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
          .map(
              item ->
                  new OrderDetailSnapshot(
                      readLong(item.get("id")),
                      readLong(item.get("orderId")),
                      readLong(item.get("foodId")),
                      readInteger(item.get("quantity"))))
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to get order details by order id: " + e.getMessage());
      return List.of();
    }
  }

  public OrderSnapshot createOrder(CreateOrderCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", command.requestId());
    requestBody.put("customerId", command.customerId());
    requestBody.put("businessId", command.businessId());
    requestBody.put("deliveryAddressId", command.deliveryAddressId());
    requestBody.put("orderTotal", command.orderTotal());
    requestBody.put("orderState", command.orderState());
    requestBody.put("voucherId", command.voucherId());
    requestBody.put("voucherDiscount", command.voucherDiscount());
    requestBody.put("pointsUsed", command.pointsUsed());
    requestBody.put("pointsDiscount", command.pointsDiscount());
    requestBody.put("walletPaid", command.walletPaid());
    requestBody.put("pointsTradeNo", command.pointsTradeNo());
    requestBody.put("orderDate", command.orderDate());
    requestBody.put("items", command.items());
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/create", requestBody);
      if (!isSuccessResponse(responseBody)) {
        return null;
      }
      return toSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to create order: " + e.getMessage());
      return null;
    }
  }

  public OrderSnapshot cancelOrder(Long orderId, Long operatorUserId) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("operatorUserId", operatorUserId);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/" + orderId + "/cancel", requestBody);
      if (!isSuccessResponse(responseBody)) {
        return null;
      }
      return toSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to cancel order: " + e.getMessage());
      return null;
    }
  }

  public OrderSnapshot updateOrderState(Long orderId, Integer orderState) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("orderState", orderState);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/" + orderId + "/state", requestBody);
      if (!isSuccessResponse(responseBody)) {
        return null;
      }
      return toSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to update order state: " + e.getMessage());
      return null;
    }
  }

  private List<OrderSnapshot> getOrderList(String path) {
    try {
      Map<?, ?> body = getInternal(path);
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
          .map(this::toSnapshot)
          .filter(snapshot -> snapshot != null)
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to get order list: " + e.getMessage());
      return List.of();
    }
  }

  private OrderSnapshot toSnapshot(Map<?, ?> data) {
    if (data == null) {
      return null;
    }
    return new OrderSnapshot(
        readLong(data.get("id")),
        readLong(data.get("customerId")),
        readLong(data.get("businessId")),
        readLong(data.get("deliveryAddressId")),
        readInteger(data.get("orderState")),
        readBigDecimal(data.get("orderTotal")),
        readLong(data.get("voucherId")),
        readBigDecimal(data.get("voucherDiscount")),
        readInteger(data.get("pointsUsed")),
        readBigDecimal(data.get("pointsDiscount")),
        readBigDecimal(data.get("walletPaid")),
        data.get("pointsTradeNo") == null ? null : String.valueOf(data.get("pointsTradeNo")),
        data.get("requestId") == null ? null : String.valueOf(data.get("requestId")),
        readDateTime(data.get("orderDate")));
  }

  public record OrderSnapshot(
      Long id,
      Long customerId,
      Long businessId,
      Long deliveryAddressId,
      Integer orderState,
      BigDecimal orderTotal,
      Long voucherId,
      BigDecimal voucherDiscount,
      Integer pointsUsed,
      BigDecimal pointsDiscount,
      BigDecimal walletPaid,
      String pointsTradeNo,
      String requestId,
      LocalDateTime orderDate) {}

  public record OrderDetailSnapshot(Long id, Long orderId, Long foodId, Integer quantity) {}

  public record CreateOrderCommand(
      String requestId,
      Long customerId,
      Long businessId,
      Long deliveryAddressId,
      BigDecimal orderTotal,
      Integer orderState,
      Long voucherId,
      BigDecimal voucherDiscount,
      Integer pointsUsed,
      BigDecimal pointsDiscount,
      BigDecimal walletPaid,
      String pointsTradeNo,
      LocalDateTime orderDate,
      List<OrderItemCommand> items) {}

  public record OrderItemCommand(Long foodId, Integer quantity) {}
}
