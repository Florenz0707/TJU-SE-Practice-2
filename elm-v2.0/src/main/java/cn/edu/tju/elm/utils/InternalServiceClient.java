package cn.edu.tju.elm.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 内部服务调用客户端工具类
 * 用于其他服务调用积分系统的内部接口
 */
@Component
public class InternalServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalServiceToken;

    public InternalServiceClient(
            @Value("${server.servlet.context-path:/elm}") String contextPath,
            @Value("${server.port:8080}") int port,
            @Value("${internal.service.token}") String internalServiceToken) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = "http://localhost:" + port + contextPath;
        this.internalServiceToken = internalServiceToken;
    }

    /**
     * 创建带有内部服务Token的请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Token", internalServiceToken);
        return headers;
    }

    /**
     * 通知订单完成（发放积分）
     */
    public Integer notifyOrderSuccess(Long userId, String bizId, Double amount, String eventTime, String extraInfo) {
        String url = baseUrl + "/api/inner/points/notify/order-success";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("bizId", bizId);
        requestBody.put("amount", amount);
        requestBody.put("eventTime", eventTime != null ? eventTime : "");
        requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
                return (Integer) ((Map<?, ?>) response.getBody()).get("data");
            }
        } catch (Exception e) {
            // 记录日志，但不抛出异常，避免影响主业务流程
            System.err.println("Failed to notify order success: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 通知评价完成（发放积分）
     */
    public Integer notifyReviewSuccess(Long userId, String bizId, Integer amount, String eventTime, String extraInfo) {
        String url = baseUrl + "/api/inner/points/notify/review-success";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("bizId", bizId);
        requestBody.put("amount", amount != null ? amount : 0);
        requestBody.put("eventTime", eventTime != null ? eventTime : "");
        requestBody.put("extraInfo", extraInfo != null ? extraInfo : "");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
                return (Integer) ((Map<?, ?>) response.getBody()).get("data");
            }
        } catch (Exception e) {
            System.err.println("Failed to notify review success: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 冻结积分
     */
    public Map<String, Object> freezePoints(Long userId, Integer points, String tempOrderId) {
        String url = baseUrl + "/api/inner/points/trade/freeze";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("points", points);
        requestBody.put("tempOrderId", tempOrderId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
                return (Map<String, Object>) ((Map<?, ?>) response.getBody()).get("data");
            }
        } catch (Exception e) {
            System.err.println("Failed to freeze points: " + e.getMessage());
        }
        return null;
    }

    /**
     * 扣除积分
     */
    public Boolean deductPoints(Long userId, String tempOrderId, String finalOrderId) {
        String url = baseUrl + "/api/inner/points/trade/deduct";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("tempOrderId", tempOrderId);
        requestBody.put("finalOrderId", finalOrderId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
                return (Boolean) ((Map<?, ?>) response.getBody()).get("data");
            }
        } catch (Exception e) {
            System.err.println("Failed to deduct points: " + e.getMessage());
        }
        return false;
    }

    /**
     * 回滚积分
     */
    public Boolean rollbackPoints(Long userId, String tempOrderId, String reason) {
        String url = baseUrl + "/api/inner/points/trade/rollback";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("tempOrderId", tempOrderId);
        requestBody.put("reason", reason != null ? reason : "");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"))) {
                return (Boolean) ((Map<?, ?>) response.getBody()).get("data");
            }
        } catch (Exception e) {
            System.err.println("Failed to rollback points: " + e.getMessage());
        }
        return false;
    }
}
