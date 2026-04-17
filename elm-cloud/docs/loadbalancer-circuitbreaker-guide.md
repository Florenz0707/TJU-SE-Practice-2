# Spring Cloud 负载均衡与熔断降级配置指南

## 一、概述

本文档介绍 elm-cloud 项目中的负载均衡和熔断降级实现方案。

### 技术选型说明

- **负载均衡**：Spring Cloud LoadBalancer（替代 Netflix Ribbon）
- **熔断降级**：Resilience4j（替代 Netflix Hystrix）
- **原因**：
  - Netflix Ribbon 和 Hystrix 已停止活跃维护
  - Spring Boot 3.x + Spring Cloud 2023.x 不再原生支持 Ribbon 和 Hystrix
  - Spring Cloud LoadBalancer 和 Resilience4j 是官方推荐的现代替代方案

## 二、负载均衡配置（Spring Cloud LoadBalancer）

### 2.1 依赖配置

在需要服务间调用的微服务的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### 2.2 RestTemplate 配置

创建带 `@LoadBalanced` 注解的 RestTemplate Bean：

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

如果需要透传 Authorization 头，可以添加拦截器：

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String token = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null && !token.isEmpty()) {
                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, token);
                }
            }
            return execution.execute(request, body);
        }));
        return restTemplate;
    }
}
```

### 2.3 使用服务名调用

在服务间调用时使用服务名而非具体的 IP 和端口：

```java
// 使用服务名而非硬编码地址
String url = "http://merchant-service/elm/api/businesses/" + businessId;
// 或者使用 lb:// 协议
String url = "lb://merchant-service/elm/api/businesses/" + businessId;
```

### 2.4 application.properties 配置

```properties
# 禁用 Ribbon，强制使用 Spring Cloud LoadBalancer
spring.cloud.loadbalancer.ribbon.enabled=false
```

## 三、熔断降级配置（Resilience4j）

### 3.1 依赖配置

在需要熔断功能的微服务的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 3.2 application.properties 配置

```properties
# Resilience4j Circuit Breaker 配置
resilience4j.circuitbreaker.configs.default.register-health-indicator=true
resilience4j.circuitbreaker.configs.default.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.configs.default.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.event-consumer-buffer-size=10

# 为特定服务配置熔断器实例
resilience4j.circuitbreaker.instances.merchantService.base-config=default
resilience4j.circuitbreaker.instances.userService.base-config=default

# Resilience4j Retry 配置
resilience4j.retry.configs.default.max-attempts=3
resilience4j.retry.configs.default.wait-duration=500ms
resilience4j.retry.configs.default.enable-exponential-backoff=true
resilience4j.retry.configs.default.exponential-backoff-multiplier=2
resilience4j.retry.instances.merchantService.base-config=default
resilience4j.retry.instances.userService.base-config=default

# Actuator 端点配置
management.endpoints.web.exposure.include=health,circuitbreakers
management.endpoint.health.show-details=always
```

### 3.3 熔断器参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `sliding-window-type` | 滑动窗口类型（COUNT_BASED 或 TIME_BASED） | COUNT_BASED |
| `sliding-window-size` | 滑动窗口大小 | 10 |
| `minimum-number-of-calls` | 熔断器计算失败率所需的最小请求数 | 5 |
| `permitted-number-of-calls-in-half-open-state` | 半开状态下允许的请求数 | 3 |
| `wait-duration-in-open-state` | 从打开到半开的等待时间 | 10s |
| `failure-rate-threshold` | 失败率阈值（百分比） | 50 |

### 3.4 代码实现

在需要熔断保护的方法上添加 `@CircuitBreaker` 和 `@Retry` 注解：

```java
@Service
public class MerchantClient {

    private static final String MERCHANT_SERVICE_CB = "merchantService";

    @CircuitBreaker(name = MERCHANT_SERVICE_CB, fallbackMethod = "getBusinessByIdFallback")
    @Retry(name = MERCHANT_SERVICE_CB)
    public Optional<BusinessDto> getBusinessById(Long businessId) {
        // 调用 merchant-service 的代码
        // ...
        throw new RuntimeException("Service unavailable"); // 模拟异常
    }

    // Fallback 方法，必须与原方法有相同的参数列表，最后加一个 Exception 参数
    public Optional<BusinessDto> getBusinessByIdFallback(Long businessId, Exception e) {
        log.warn("Fallback triggered for getBusinessById({}): {}", businessId, e.getMessage());
        return Optional.empty();
    }
}
```

### 3.5 熔断器状态说明

熔断器有三种状态：
1. **CLOSED（关闭）**：正常状态，请求正常通过
2. **OPEN（打开）**：错误率超过阈值，熔断器打开，直接拒绝请求
3. **HALF_OPEN（半开）**：等待一段时间后，允许少量请求通过，测试服务是否恢复

## 四、已配置的服务

### 4.1 product-service
- **熔断器实例**：`merchantService`
- **保护的方法**：
  - `getBusinessById()` - 获取商家信息
  - `getMyBusinesses()` - 获取我的商家列表

### 4.2 merchant-service
- **熔断器实例**：`userService`
- **保护的方法**：
  - `getUserById()` - 获取用户信息
  - `updateUser()` - 更新用户信息

### 4.3 cart-service
- 已添加负载均衡依赖和 Actuator

### 4.4 order-service
- 已添加负载均衡依赖和 Actuator

## 五、监控与验证

### 5.1 Actuator 端点

访问以下端点查看熔断器状态：

```
# 健康检查
http://<service-host>:<port>/actuator/health

# 熔断器详情
http://<service-host>:<port>/actuator/circuitbreakers
```

在 Docker 环境中，可以通过网关或直接访问服务端口（如果暴露）来访问这些端点。

### 5.2 健康检查响应示例

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "merchantService": {
          "status": "UP",
          "details": {
            "failureRate": "-1.0%",
            "slowCallRate": "-1.0%",
            "state": "CLOSED"
          }
        }
      }
    }
  }
}
```

## 六、Docker 部署说明

由于项目采用 Docker 部署，需要注意以下几点：

1. **服务发现**：确保所有服务都能连接到 Eureka Server
2. **网络配置**：所有服务应在同一 Docker 网络中，能够通过服务名互相访问
3. **健康检查**：可以在 docker-compose.yml 中配置健康检查
4. **多实例部署**：为了测试负载均衡，可以启动多个实例

### docker-compose.yml 示例（多实例）

```yaml
services:
  merchant-service:
    build:
      context: ..
      dockerfile: merchant-service/Dockerfile
    deploy:
      replicas: 2  # 启动2个实例测试负载均衡
    depends_on:
      - mysql
      - eureka-server
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - DB_URL=jdbc:mysql://mysql:3306/elm_merchant?...
```

## 七、扩展其他服务

如果需要为其他服务添加熔断保护，按以下步骤：

1. 在 `pom.xml` 中添加 Resilience4j 依赖
2. 在 `application.properties` 中添加熔断器配置
3. 在需要保护的方法上添加 `@CircuitBreaker` 和 `@Retry` 注解
4. 实现对应的 Fallback 方法

## 八、总结

本方案实现了：
- ✅ 基于 Spring Cloud LoadBalancer 的客户端负载均衡
- ✅ 基于 Resilience4j 的熔断降级机制
- ✅ 支持重试和 Fallback
- ✅ Actuator 监控端点
- ✅ 与 Docker 部署兼容

通过这些配置，elm-cloud 微服务系统具备了高可用性和容错能力，能够在服务故障时优雅降级，防止级联故障。
