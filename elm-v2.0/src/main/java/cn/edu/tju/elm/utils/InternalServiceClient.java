package cn.edu.tju.elm.utils;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 内部服务调用客户端工具类 用于其他服务调用积分系统的内部接口 */
@Component
public class InternalServiceClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalServiceClient(
      @Value("${points.service.url:http://localhost:8080/elm}") String pointsServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = new RestTemplate();
    this.baseUrl =
        pointsServiceUrl.endsWith("/")
            ? pointsServiceUrl.substring(0, pointsServiceUrl.length() - 1)
            : pointsServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  /** 创建带有内部服务Token的请求头 */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    return headers;
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

  private Integer readIntegerData(Map<?, ?> body) {
    if (body == null) {
      return null;
    }
    Object data = body.get("data");
    if (data instanceof Integer i) {
      return i;
    }
    if (data instanceof Number n) {
      return n.intValue();
    }
    return null;
  }

  /** 通知订单完成（发放积分） */
  public Integer notifyOrderSuccess(
      Long userId, String bizId, Double amount, String eventTime, String extraInfo) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("bizId", bizId);
    requestBody.put("amount", amount);
    requestBody.put("eventTime", eventTime != null ? eventTime : "");
    requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");

    try {
      Map<?, ?> responseBody = postInternal("/api/inner/points/notify/order-success", requestBody);
      if (isSuccessResponse(responseBody)) {
        Integer points = readIntegerData(responseBody);
        return points != null ? points : 0;
      }
    } catch (Exception e) {
      // 记录日志，但不抛出异常，避免影响主业务流程
      System.err.println("Failed to notify order success: " + e.getMessage());
    }
    return 0;
  }

  /** 通知评价完成（发放积分） */
  public Integer notifyReviewSuccess(
      Long userId, String bizId, Integer amount, String eventTime, String extraInfo) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("bizId", bizId);
    requestBody.put("amount", amount != null ? amount : 0);
    requestBody.put("eventTime", eventTime != null ? eventTime : "");
    requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");

    try {
      Map<?, ?> responseBody = postInternal("/api/inner/points/notify/review-success", requestBody);
      if (isSuccessResponse(responseBody)) {
        Integer points = readIntegerData(responseBody);
        return points != null ? points : 0;
      }
    } catch (Exception e) {
      System.err.println("Failed to notify review success: " + e.getMessage());
    }
    return 0;
  }

  /** 通知订单完成（用于Outbox可靠投递） */
  public boolean notifyOrderSuccessReliable(
      Long userId, String bizId, Double amount, String eventTime, String extraInfo) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("bizId", bizId);
    requestBody.put("amount", amount);
    requestBody.put("eventTime", eventTime != null ? eventTime : "");
    requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/points/notify/order-success", requestBody);
      return isSuccessResponse(responseBody);
    } catch (Exception e) {
      System.err.println("Failed to notify order success reliably: " + e.getMessage());
      return false;
    }
  }

  /** 通知评价完成（用于Outbox可靠投递） */
  public boolean notifyReviewSuccessReliable(
      Long userId, String bizId, Integer amount, String eventTime, String extraInfo) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("bizId", bizId);
    requestBody.put("amount", amount != null ? amount : 0);
    requestBody.put("eventTime", eventTime != null ? eventTime : "");
    requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/points/notify/review-success", requestBody);
      return isSuccessResponse(responseBody);
    } catch (Exception e) {
      System.err.println("Failed to notify review success reliably: " + e.getMessage());
      return false;
    }
  }

  /** 冻结积分 */
  public Map<String, Object> freezePoints(Long userId, Integer points, String tempOrderId) {
    String url = baseUrl + "/api/inner/points/trade/freeze";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("points", points);
    requestBody.put("tempOrderId", tempOrderId);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getBody() != null
          && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
        return (Map<String, Object>) ((Map<?, ?>) response.getBody()).get("data");
      }
    } catch (Exception e) {
      System.err.println("Failed to freeze points: " + e.getMessage());
    }
    return null;
  }

  /** 扣除积分 */
  public Boolean deductPoints(Long userId, String tempOrderId, String finalOrderId) {
    String url = baseUrl + "/api/inner/points/trade/deduct";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("tempOrderId", tempOrderId);
    requestBody.put("finalOrderId", finalOrderId);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getBody() != null
          && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
        return (Boolean) ((Map<?, ?>) response.getBody()).get("data");
      }
    } catch (Exception e) {
      System.err.println("Failed to deduct points: " + e.getMessage());
    }
    return false;
  }

  /** 回滚积分 */
  public Boolean rollbackPoints(Long userId, String tempOrderId, String reason) {
    String url = baseUrl + "/api/inner/points/trade/rollback";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("tempOrderId", tempOrderId);
    requestBody.put("reason", reason != null ? reason : "");

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getBody() != null
          && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
        return (Boolean) ((Map<?, ?>) response.getBody()).get("data");
      }
    } catch (Exception e) {
      System.err.println("Failed to rollback points: " + e.getMessage());
    }
    return false;
  }

  /** 返还已扣减积分（用于订单取消） */
  public boolean refundDeductedPoints(Long userId, String orderBizId, String reason) {
    String url = baseUrl + "/api/inner/points/trade/refund";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("orderBizId", orderBizId);
    requestBody.put("reason", reason != null ? reason : "");

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getBody() != null
          && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
        Object data = ((Map<?, ?>) response.getBody()).get("data");
        return Boolean.TRUE.equals(data);
      }
    } catch (Exception e) {
      System.err.println("Failed to refund deducted points: " + e.getMessage());
    }
    return false;
  }

  /** 删除评价后扣除积分 */
  public boolean notifyReviewDeleted(Long userId, String reviewId) {
    String url = baseUrl + "/api/inner/points/notify/review-deleted";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("reviewId", reviewId);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getBody() != null
          && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
        Object data = ((Map<?, ?>) response.getBody()).get("data");
        return Boolean.TRUE.equals(data);
      }
    } catch (Exception e) {
      System.err.println("Failed to notify review deleted: " + e.getMessage());
    }
    return false;
  }
}
