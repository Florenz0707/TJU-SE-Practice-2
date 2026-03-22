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

  private static Boolean readBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    return Boolean.parseBoolean(String.valueOf(value));
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

  public PagedOrderSnapshot getOrdersByCustomerId(Long customerId, int page, int size) {
    return getOrderPage(
        "/api/inner/order/customer/" + customerId + "/page?page=" + page + "&size=" + size);
  }

  public PagedOrderSnapshot getOrdersByBusinessId(Long businessId, int page, int size) {
    return getOrderPage(
        "/api/inner/order/business/" + businessId + "/page?page=" + page + "&size=" + size);
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

  public AddressSnapshot createAddress(CreateAddressCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customerId", command.customerId());
    requestBody.put("contactName", command.contactName());
    requestBody.put("contactSex", command.contactSex());
    requestBody.put("contactTel", command.contactTel());
    requestBody.put("address", command.address());
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/address", requestBody);
      return toAddressSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to create address: " + e.getMessage());
      return null;
    }
  }

  public AddressSnapshot getAddressById(Long addressId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/address/" + addressId);
      return toAddressSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get address by id: " + e.getMessage());
      return null;
    }
  }

  public List<AddressSnapshot> getAddressesByCustomerId(Long customerId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/address/customer/" + customerId);
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
      String path = "/api/inner/order/address/" + addressId;
      String url = baseUrl + path;
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
      String path = "/api/inner/order/address/" + addressId;
      String url = baseUrl + path;
      HttpEntity<Void> request = new HttpEntity<>(createHeaders());
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
      Map<?, ?> body = response.getBody();
      if (!isSuccessResponse(body)) {
        return false;
      }
      Object data = body == null ? null : body.get("data");
      if (data instanceof Boolean value) {
        return value;
      }
      return false;
    } catch (Exception e) {
      System.err.println("Failed to delete address: " + e.getMessage());
      return false;
    }
  }

  public CartSnapshot createCart(CreateCartCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("foodId", command.foodId());
    requestBody.put("customerId", command.customerId());
    requestBody.put("businessId", command.businessId());
    requestBody.put("quantity", command.quantity());
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/cart", requestBody);
      return toCartSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to create cart: " + e.getMessage());
      return null;
    }
  }

  public CartSnapshot getCartById(Long cartId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/cart/" + cartId);
      return toCartSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get cart by id: " + e.getMessage());
      return null;
    }
  }

  public List<CartSnapshot> getCartsByCustomerId(Long customerId) {
    return getCartList("/api/inner/order/cart/customer/" + customerId);
  }

  public List<CartSnapshot> getCartsByBusinessAndCustomerId(Long businessId, Long customerId) {
    return getCartList("/api/inner/order/cart/business/" + businessId + "/customer/" + customerId);
  }

  public CartSnapshot updateCartQuantity(Long cartId, Integer quantity) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("quantity", quantity);
    try {
      Map<?, ?> responseBody =
          postInternal("/api/inner/order/cart/" + cartId + "/quantity", requestBody);
      return toCartSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to update cart quantity: " + e.getMessage());
      return null;
    }
  }

  public boolean deleteCart(Long cartId) {
    try {
      String path = "/api/inner/order/cart/" + cartId;
      String url = baseUrl + path;
      HttpEntity<Void> request = new HttpEntity<>(createHeaders());
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
      Map<?, ?> body = response.getBody();
      if (!isSuccessResponse(body)) {
        return false;
      }
      Object data = body == null ? null : body.get("data");
      if (data instanceof Boolean value) {
        return value;
      }
      return false;
    } catch (Exception e) {
      System.err.println("Failed to delete cart: " + e.getMessage());
      return false;
    }
  }

  public ReviewSnapshot createReview(CreateReviewCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customerId", command.customerId());
    requestBody.put("businessId", command.businessId());
    requestBody.put("orderId", command.orderId());
    requestBody.put("anonymous", command.anonymous());
    requestBody.put("stars", command.stars());
    requestBody.put("content", command.content());
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/order/review", requestBody);
      return toReviewSnapshot(readMapData(responseBody));
    } catch (Exception e) {
      System.err.println("Failed to create review: " + e.getMessage());
      return null;
    }
  }

  public ReviewSnapshot updateReview(Long reviewId, UpdateReviewCommand command) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("stars", command.stars());
    requestBody.put("content", command.content());
    requestBody.put("anonymous", command.anonymous());
    try {
      String path = "/api/inner/order/review/" + reviewId;
      String url = baseUrl + path;
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);
      return toReviewSnapshot(readMapData(response.getBody()));
    } catch (Exception e) {
      System.err.println("Failed to update review: " + e.getMessage());
      return null;
    }
  }

  public ReviewSnapshot getReviewById(Long reviewId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/review/" + reviewId);
      return toReviewSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get review by id: " + e.getMessage());
      return null;
    }
  }

  public ReviewSnapshot getReviewByOrderId(Long orderId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/order/review/order/" + orderId);
      return toReviewSnapshot(readMapData(body));
    } catch (Exception e) {
      System.err.println("Failed to get review by order id: " + e.getMessage());
      return null;
    }
  }

  public List<ReviewSnapshot> getReviewsByCustomerId(Long customerId) {
    return getReviewList("/api/inner/order/review/customer/" + customerId);
  }

  public List<ReviewSnapshot> getReviewsByBusinessId(Long businessId) {
    return getReviewList("/api/inner/order/review/business/" + businessId);
  }

  public ReviewSnapshot deleteReview(Long reviewId) {
    try {
      String path = "/api/inner/order/review/" + reviewId;
      String url = baseUrl + path;
      HttpEntity<Void> request = new HttpEntity<>(createHeaders());
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
      return toReviewSnapshot(readMapData(response.getBody()));
    } catch (Exception e) {
      System.err.println("Failed to delete review: " + e.getMessage());
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

  private PagedOrderSnapshot getOrderPage(String path) {
    try {
      Map<?, ?> body = getInternal(path);
      Map<?, ?> data = readMapData(body);
      if (data == null) {
        return new PagedOrderSnapshot(List.of(), 0L, 1, 10);
      }
      Object ordersObj = data.get("orders");
      List<OrderSnapshot> orders =
          ordersObj instanceof List<?> list
              ? list.stream()
                  .filter(Map.class::isInstance)
                  .map(Map.class::cast)
                  .map(this::toSnapshot)
                  .filter(snapshot -> snapshot != null)
                  .toList()
              : List.of();
      Long total = readLong(data.get("total"));
      Integer page = readInteger(data.get("page"));
      Integer size = readInteger(data.get("size"));
      return new PagedOrderSnapshot(
          orders, total == null ? 0L : total, page == null ? 1 : page, size == null ? 10 : size);
    } catch (Exception e) {
      System.err.println("Failed to get order page: " + e.getMessage());
      return new PagedOrderSnapshot(List.of(), 0L, 1, 10);
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

  private CartSnapshot toCartSnapshot(Map<?, ?> data) {
    if (data == null) {
      return null;
    }
    return new CartSnapshot(
        readLong(data.get("id")),
        readLong(data.get("foodId")),
        readLong(data.get("customerId")),
        readLong(data.get("businessId")),
        readInteger(data.get("quantity")));
  }

  private List<CartSnapshot> getCartList(String path) {
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
          .map(this::toCartSnapshot)
          .filter(snapshot -> snapshot != null)
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to get cart list: " + e.getMessage());
      return List.of();
    }
  }

  private ReviewSnapshot toReviewSnapshot(Map<?, ?> data) {
    if (data == null) {
      return null;
    }
    return new ReviewSnapshot(
        readLong(data.get("id")),
        readLong(data.get("customerId")),
        readLong(data.get("businessId")),
        readLong(data.get("orderId")),
        readBoolean(data.get("anonymous")),
        readInteger(data.get("stars")),
        data.get("content") == null ? null : String.valueOf(data.get("content")));
  }

  private List<ReviewSnapshot> getReviewList(String path) {
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
          .map(this::toReviewSnapshot)
          .filter(snapshot -> snapshot != null)
          .toList();
    } catch (Exception e) {
      System.err.println("Failed to get review list: " + e.getMessage());
      return List.of();
    }
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

  public record CartSnapshot(
      Long id, Long foodId, Long customerId, Long businessId, Integer quantity) {}

  public record CreateCartCommand(
      Long foodId, Long customerId, Long businessId, Integer quantity) {}

  public record ReviewSnapshot(
      Long id,
      Long customerId,
      Long businessId,
      Long orderId,
      Boolean anonymous,
      Integer stars,
      String content) {}

  public record CreateReviewCommand(
      Long customerId,
      Long businessId,
      Long orderId,
      Boolean anonymous,
      Integer stars,
      String content) {}

  public record UpdateReviewCommand(Integer stars, String content, Boolean anonymous) {}

  public record PagedOrderSnapshot(
      List<OrderSnapshot> orders, Long total, Integer page, Integer size) {}

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
