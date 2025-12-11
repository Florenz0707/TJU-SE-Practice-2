# 内部服务集成指南

本文档说明如何在其他服务中调用积分系统的内部接口。

## 配置

### 1. 配置内部服务Token

在 `application.properties` 中配置内部服务Token：

```properties
# Internal service token for inter-service communication
internal.service.token=internal-service-secret-token-2024
```

**重要**：在生产环境中，请使用强密码并妥善保管此Token。

## 调用方式

### 方式一：使用 InternalServiceClient 工具类（推荐）

如果积分系统和其他服务在同一个应用中，可以直接注入 `InternalServiceClient`：

```java
@Service
public class OrderService {
    private final InternalServiceClient pointsClient;

    public OrderService(InternalServiceClient pointsClient) {
        this.pointsClient = pointsClient;
    }

    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getOrderState().equals(OrderState.COMPLETE)) {
            // 通知积分系统订单完成
            Integer points = pointsClient.notifyOrderSuccess(
                order.getCustomer().getId(),
                "ORD_" + order.getId(),
                order.getOrderTotal().doubleValue(),
                LocalDateTime.now().toString(),
                "{\"orderId\":" + order.getId() + "}"
            );
            // 记录日志或处理积分发放结果
        }
    }
}
```

### 方式二：使用 HTTP 客户端直接调用

如果积分系统是独立的微服务，需要使用 HTTP 客户端调用：

#### 使用 RestTemplate

```java
@Service
public class OrderService {
    private final RestTemplate restTemplate;

    @Value("${points.service.url:http://localhost:8080/elm}")
    private String pointsServiceUrl;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getOrderState().equals(OrderState.COMPLETE)) {
            String url = pointsServiceUrl + "/api/inner/points/notify/order-success";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service-Token", internalServiceToken);

            Map<String, Object> requestBody = Map.of(
                "userId", order.getCustomer().getId(),
                "bizId", "ORD_" + order.getId(),
                "amount", order.getOrderTotal().doubleValue(),
                "eventTime", LocalDateTime.now().toString(),
                "extraInfo", "{\"orderId\":" + order.getId() + "}"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                if (response.getBody() != null) {
                    Map<?, ?> body = response.getBody();
                    if (Boolean.TRUE.equals(body.get("success"))) {
                        Integer points = (Integer) body.get("data");
                        // 处理积分发放结果
                    }
                }
            } catch (Exception e) {
                // 记录日志，但不影响主业务流程
                log.error("Failed to notify points service", e);
            }
        }
    }
}
```

#### 使用 WebClient（Spring WebFlux）

```java
@Service
public class OrderService {
    private final WebClient webClient;

    @Value("${points.service.url:http://localhost:8080/elm}")
    private String pointsServiceUrl;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    public OrderService() {
        this.webClient = WebClient.builder()
            .baseUrl(pointsServiceUrl)
            .defaultHeader("X-Internal-Service-Token", internalServiceToken)
            .build();
    }

    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getOrderState().equals(OrderState.COMPLETE)) {
            webClient.post()
                .uri("/api/inner/points/notify/order-success")
                .bodyValue(Map.of(
                    "userId", order.getCustomer().getId(),
                    "bizId", "ORD_" + order.getId(),
                    "amount", order.getOrderTotal().doubleValue(),
                    "eventTime", LocalDateTime.now().toString(),
                    "extraInfo", "{\"orderId\":" + order.getId() + "}"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(
                    response -> {
                        if (Boolean.TRUE.equals(response.get("success"))) {
                            Integer points = (Integer) response.get("data");
                            // 处理积分发放结果
                        }
                    },
                    error -> log.error("Failed to notify points service", error)
                );
        }
    }
}
```

## 接口说明

### 1. 订单完成通知

**接口**：`POST /api/inner/points/notify/order-success`

**请求头**：
```
X-Internal-Service-Token: {internal.service.token}
Content-Type: application/json
```

**请求体**：
```json
{
  "userId": 1001,
  "bizId": "ORD_20231111_001",
  "amount": 99.5,
  "eventTime": "2023-11-11T10:00:00Z",
  "extraInfo": "{\"isDouble11\": true}"
}
```

**响应**：
```json
{
  "success": true,
  "code": "200",
  "message": "积分发放成功",
  "data": 99
}
```

### 2. 评价完成通知

**接口**：`POST /api/inner/points/notify/review-success`

**请求头**：
```
X-Internal-Service-Token: {internal.service.token}
Content-Type: application/json
```

**请求体**：
```json
{
  "userId": 1001,
  "bizId": "REV_556677",
  "amount": 0,
  "eventTime": "2023-11-12T10:00:00Z",
  "extraInfo": "{\"hasImage\": true, \"wordCount\": 50}"
}
```

**响应**：
```json
{
  "success": true,
  "code": "200",
  "message": "评价积分发放成功",
  "data": 10
}
```

### 3. 积分冻结

**接口**：`POST /api/inner/points/trade/freeze`

**请求体**：
```json
{
  "userId": 1001,
  "points": 500,
  "tempOrderId": "TEMP_ORD_123456"
}
```

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "success": true,
    "pointsUsed": 500,
    "moneySaved": 5,
    "balanceSnap": 2000,
    "message": "锁定成功"
  }
}
```

### 4. 积分扣除

**接口**：`POST /api/inner/points/trade/deduct`

**请求体**：
```json
{
  "userId": 1001,
  "tempOrderId": "TEMP_ORD_123456",
  "finalOrderId": "ORD_20231111_001"
}
```

### 5. 积分回滚

**接口**：`POST /api/inner/points/trade/rollback`

**请求体**：
```json
{
  "userId": 1001,
  "tempOrderId": "TEMP_ORD_123456",
  "reason": "USER_CANCEL"
}
```

## 集成示例

### 在订单服务中集成

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PointsService pointsService; // 如果同应用，直接注入Service

    // 订单完成时发放积分
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setOrderState(OrderState.COMPLETE);
            orderRepository.save(order);

            // 调用积分服务
            try {
                pointsService.notifyOrderSuccess(
                    order.getCustomer().getId(),
                    "ORD_" + order.getId(),
                    order.getOrderTotal().doubleValue(),
                    LocalDateTime.now().toString(),
                    "{\"orderId\":" + order.getId() + "}"
                );
            } catch (Exception e) {
                // 记录日志，但不影响订单完成流程
                log.error("Failed to award points for order: " + orderId, e);
            }
        }
    }
}
```

### 在评价服务中集成

```java
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PointsService pointsService;

    public void addReview(Review review) {
        reviewRepository.save(review);

        // 调用积分服务
        try {
            pointsService.notifyReviewSuccess(
                review.getCustomer().getId(),
                "REV_" + review.getId(),
                0, // 评价积分由规则决定
                LocalDateTime.now().toString(),
                "{\"reviewId\":" + review.getId() + "}"
            );
        } catch (Exception e) {
            log.error("Failed to award points for review: " + review.getId(), e);
        }
    }
}
```

## 安全注意事项

1. **Token 安全**：
   - 内部服务Token应该足够复杂
   - 不要将Token提交到版本控制系统
   - 定期轮换Token

2. **网络安全**：
   - 在生产环境中，内部服务调用应该通过内网进行
   - 考虑使用HTTPS加密通信

3. **错误处理**：
   - 积分服务调用失败不应该影响主业务流程
   - 记录详细的错误日志以便排查问题
   - 考虑实现重试机制

4. **幂等性**：
   - 确保相同业务ID的积分操作是幂等的
   - 避免重复发放积分

## 测试

### 使用 curl 测试内部接口

```bash
# 订单完成通知
curl -X POST http://localhost:8080/elm/api/inner/points/notify/order-success \
  -H "Content-Type: application/json" \
  -H "X-Internal-Service-Token: internal-service-secret-token-2024" \
  -d '{
    "userId": 1,
    "bizId": "ORD_TEST_001",
    "amount": 100.0,
    "eventTime": "2024-01-01T10:00:00Z",
    "extraInfo": "{}"
  }'

# 评价完成通知
curl -X POST http://localhost:8080/elm/api/inner/points/notify/review-success \
  -H "Content-Type: application/json" \
  -H "X-Internal-Service-Token: internal-service-secret-token-2024" \
  -d '{
    "userId": 1,
    "bizId": "REV_TEST_001",
    "amount": 0,
    "eventTime": "2024-01-01T10:00:00Z",
    "extraInfo": "{}"
  }'
```

## 故障排查

1. **401 Unauthorized**：
   - 检查请求头中是否包含 `X-Internal-Service-Token`
   - 验证Token是否与配置中的Token一致

2. **500 Internal Server Error**：
   - 检查积分规则是否已配置
   - 查看服务器日志获取详细错误信息

3. **积分未发放**：
   - 确认对应渠道的积分规则已启用
   - 检查用户积分账户是否存在
   - 验证业务ID是否唯一
