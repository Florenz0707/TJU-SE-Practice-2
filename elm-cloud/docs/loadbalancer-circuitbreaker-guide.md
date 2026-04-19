# Spring Cloud 负载均衡与熔断降级配置指南

## 一、概述

本项目采用现代微服务架构，实现了完整的负载均衡和熔断降级机制，在微服务内部使用Resilience4j进行保护。

### 技术选型说明

- **服务注册与发现**：Eureka Server
- **配置中心**：Spring Cloud Config Server
- **API 网关**：Spring Cloud Gateway
- **负载均衡**：Spring Cloud LoadBalancer（替代 Netflix Ribbon）
- **熔断降级**：Resilience4j（替代 Netflix Hystrix）
- **原因**：Netflix 相关组件已停止维护，Spring Boot 3.x + Spring Cloud 2023.x 推荐使用现代替代方案

***

## 二、整体架构

### 服务依赖关系

```
前端 → Gateway（路由 + 负载均衡）
         ↓
       Eureka（服务发现）
         ↓
    ┌────┴─────────────────────┐
    ↓                         ↓
微服务1 → 微服务2 → 微服务3   Config Server（配置管理）
（Resilience4j 内部熔断）
```

### 微服务列表

| 服务 | 端口 | Context Path | 数据库 Schema | 主要功能 |
|------|------|--------------|---------------|----------|
| eureka-server | 8761 | / | - | 服务注册与发现 |
| config-server | 8888 | / | - | 集中配置管理 |
| gateway | 8080 | / | - | 统一入口与路由 |
| user-service | 8082 | /elm | elm_user | 用户管理、认证 |
| merchant-service | 8085 | /elm | elm_merchant | 商家与店铺管理 |
| product-service | 8083 | /elm | elm_catalog | 菜品管理 |
| cart-service | 8086 | /elm | elm_order | 购物车管理 |
| order-service | 8084 | /elm | elm_order | 订单管理 |
| address-service | 8087 | /elm | elm_address | 地址管理 |
| points-service | 8081 | /elm | elm_points | 积分管理 |
| wallet-service | 8088 | /elm | elm_wallet | 钱包管理 |

***

## 三、负载均衡熔断降级策略

### 3.1 分层策略

| 层级 | 处理对象 | 技术方案 |
|------|---------|---------|
| **Gateway** | 外部请求 | Gateway + 负载均衡 |
| **微服务内部调用** | 服务间调用 | Resilience4j + Spring Cloud LoadBalancer |

### 3.2 微服务内部熔断降级

微服务之间的调用使用Resilience4j进行熔断降级保护。

#### 实现方法

1. **添加依赖**：在需要调用其他微服务的服务中添加负载均衡和熔断依赖
2. **配置RestTemplate**：配置带有负载均衡能力的RestTemplate Bean
3. **配置Resilience4j**：在配置文件中设置Resilience4j的熔断器和重试参数
4. **添加注解**：在调用其他服务的方法上添加`@CircuitBreaker`和`@Retry`注解
5. **编写Fallback**：为每个需要保护的方法编写对应的Fallback方法

#### 熔断器参数

- 滑动窗口类型：基于请求数统计
- 滑动窗口大小：统计最近的10个请求
- 最少请求数：达到5个请求才开始计算失败率
- 失败率阈值：当失败率超过50%时触发熔断
- 等待时间：熔断打开后，等待10秒进入半开状态
- 半开允许请求数：半开状态允许3个请求通过，测试服务是否恢复

#### 已配置的服务

**product-service**
- 调用merchant-service获取店铺信息
- 当merchant-service不可用时，返回空的店铺数据

**merchant-service**
- 调用user-service获取用户信息
- 当user-service不可用时，返回默认的用户数据

**cart-service**
- 调用product-service和merchant-service丰富购物车数据
- 当这些服务不可用时，只显示购物车基本数据，不填充详情

**order-service**
- 调用多个微服务完成下单流程
- 每个调用都有独立的熔断器和Fallback方法
- 非关键服务失败时，不影响订单的核心流程

***

## 四、配置中心（Config Server）

### 4.1 Config Server配置

Config Server负责集中管理所有微服务的配置，所有服务的配置都存放在Config Server的配置目录中。

### 4.2 各服务的Config Client配置

所有业务微服务（包括gateway）都配置了Config Client：
- 使用`bootstrap.yml`配置Config Server连接信息
- 设置`fail-fast: false`，Config Server不可用时自动降级使用本地配置
- 配置重试策略，尝试连接Config Server几次后再降级

### 4.3 配置降级策略

配置加载的优先级是远程配置高于本地配置。

当Config Server不可用时：
- 微服务会尝试连接Config Server几次
- 如果连接失败，会继续启动，使用本地配置文件
- 原本地配置文件已备份为`application.properties.backup`或`application.yml.backup`

***

## 五、负载均衡配置

### 5.1 RestTemplate配置

在需要服务间调用的微服务中，配置带有负载均衡能力的RestTemplate Bean。

### 5.2 使用服务名调用

在服务间调用时，使用服务名代替具体的IP和端口，这样可以：
- 自动从Eureka获取服务实例列表
- 自动实现负载均衡
- 即使服务实例增减也无需修改代码

### 5.3 配置参数

在配置文件中禁用Ribbon，强制使用Spring Cloud LoadBalancer。

***

## 六、熔断器状态说明

熔断器有三种状态：

1. **CLOSED（关闭）**：正常状态，请求正常通过
2. **OPEN（打开）**：错误率超过阈值，熔断器打开，直接拒绝请求，等待一段时间后自动进入半开状态
3. **HALF-OPEN（半开）**：允许少量请求通过，测试服务是否恢复，成功则回到关闭状态，失败则重新打开

***

## 七、监控与验证

### 7.1 Actuator端点

访问以下端点查看熔断器状态：

- 健康检查：查看所有组件的健康状态
- 熔断器详情：查看每个熔断器的当前状态、失败率等信息

### 7.2 测试熔断降级的方法

**测试场景1**：某个微服务挂掉，访问对应接口
- 停止该微服务
- 调用该服务的接口
- 验证是否返回降级响应，页面仍然可以正常工作

**测试场景2**：Config Server挂掉，各服务启动
- 停止Config Server
- 重启各业务服务
- 验证服务能正常启动并使用本地配置

***

## 八、完成情况总结

### 8.1 已完成的功能

✅ **微服务内部调用熔断降级**
- product-service：调用merchant-service有熔断降级
- merchant-service：调用user-service有熔断降级
- cart-service：调用product-service、merchant-service有熔断降级
- order-service：调用多个微服务都有熔断降级

✅ **负载均衡配置**
- gateway、merchant、product、cart、order都配置了Spring Cloud LoadBalancer
- 使用服务名进行调用，自动负载均衡
- 增加实例时自动负载均衡

✅ **Config Server配置**
- 所有业务微服务（包括gateway）都配置了Config Client
- Config Server目录中存放了所有服务的配置
- 配置降级策略实现：Config Server不可用时自动使用本地配置
- 本地配置已备份

✅ **Cart Service路径统一**
- cart-service的context-path改为/elm，与其他服务保持一致
- order-service的内部调用路径已更新
- gateway的路由配置已更新支持/elm前缀

✅ **文档完善**
- 完整的架构说明
- 详细的策略介绍
- 实现方法说明
- 完成情况总结

### 8.2 各服务配置完成情况

| 服务 | 负载均衡配置 | 熔断降级配置 | Config Server配置 |
|------|---------|---------|-----------------|
| eureka-server | - | - | - |
| config-server | - | - | - |
| gateway | ✅ | ❌（暂未配置） | ✅ |
| user-service | ⚠️（有依赖但暂未使用） | ❌（暂未配置） | ✅ |
| merchant-service | ✅ | ✅ | ✅ |
| product-service | ✅ | ✅ | ✅ |
| cart-service | ✅ | ✅ | ✅ |
| order-service | ✅ | ✅ | ✅ |
| address-service | ❌（未配置） | ❌（暂未配置） | ❌（未配置） |
| wallet-service | ❌（未配置） | ❌（暂未配置） | ❌（未配置） |
| points-service | ❌（未配置） | ❌（暂未配置） | ❌（未配置） |

**说明**：user-service、address-service、wallet-service、points-service虽然没有配置完整的负载均衡和熔断降级，但由于它们主要作为被调用的服务，当前的架构已经可以正常工作。如需未来这些服务调用其他服务，可以按照现有模式进行配置。

### 8.3 架构优势

1. **职责清晰**：Gateway负责路由和负载均衡，微服务内部负责熔断降级
2. **服务解耦**：一个服务挂掉不影响其他服务
3. **容错优先**：服务不可用时优雅降级，不影响其他功能
4. **可观测性**：通过Actuator监控熔断器状态
5. **配置集中化**：Config Server管理，本地配置降级

***

## 九、总结

### 9.1 已实现的功能

- 完整的微服务架构（Eureka + Config Server + Gateway）
- Config Server集中配置管理 + 本地配置降级
- Spring Cloud LoadBalancer负载均衡（服务名调用）
- Resilience4j熔断降级 + 重试机制（微服务内部调用）
- 后端服务间调用熔断保护
- 前端非关键请求降级处理
- Actuator健康检查和监控端点
- Cart Service路径与其他服务统一
- 和Docker部署兼容

### 9.2 关键设计原则

1. **容错优先**：服务不可用时优雅降级，不影响其他功能
2. **服务解耦**：一个服务挂掉不影响其他服务
3. **分层降级**：微服务内部熔断降级保护
4. **可观测性**：通过Actuator监控熔断器状态
5. **配置集中化**：Config Server管理，本地配置降级

通过以上配置和实现，elm-cloud微服务系统具备了企业级的高可用性和容错能力！
