# 微服务验收收口 Todo（2026-03-31）

## 目标

围绕老师本次会重点检查的微服务能力做最后收口，重点不是再扩业务功能，而是提升：

1. 启动稳定性
2. 服务发现与负载均衡一致性
3. 配置中心与刷新链路可解释性
4. 内部调用安全边界
5. 运维与验收可观测性

## Todo 列表

- [x] 为 `config-server`、`discovery-server`、`elm-v2`、`gateway-service` 增加容器健康检查
- [x] 将关键 `depends_on` 从 `service_started` 收紧到 `service_healthy`
- [x] 将 `gateway-service` 在 compose 下的直通服务路由切换为 `lb://service-id`
- [x] 收口 `INTERNAL_SERVICE_TOKEN` 策略，去掉默认弱口令回退或至少区分 dev / acceptance
- [x] 补一份“微服务验收答辩提纲”，覆盖 Eureka、Config、Gateway、Feign、熔断降级、内部鉴权
- [x] 评估并补充配置刷新广播的失败恢复说明与人工兜底步骤
- [x] 评估是否需要把 `config-server` 从 `native` 模式再补一段“等价于集中配置仓”的验收说明
- [x] 视时间补充网关 `lb://` 路由级集成测试，避免只在 compose 配置层体现服务发现
- [x] 去掉源码层 `INTERNAL_SERVICE_TOKEN` / `CONFIG_REFRESH_TOKEN` 固定 fallback，仅保留显式配置与 local profile 本地兜底
- [x] 将测试中的 `@MockBean` 迁移到新注解，清理 Spring Boot 3.5 弃用告警
- [x] 为核心业务服务补齐健康检查，并把 `service_started` 依赖进一步收紧到 `service_healthy`

## 本轮已完成

### 1. 启动稳定性

- 已为 `config-server`、`discovery-server-a`、`discovery-server-b`、`elm-v2`、`gateway-service` 规划健康检查链路
- 已将关键启动依赖切换为基于健康状态的串联，降低冷启动竞态

### 2. 服务发现一致性

- 已将 `gateway-service` 在 compose 场景下访问 `elm-v2` 与各微服务的直通路由环境变量切换为 `lb://service-id`
- 这样老师现场如果检查网关直通能力，不再只能看到固定地址转发，而是能看到基于服务发现的负载均衡入口

### 3. 内部鉴权边界

- 已将 compose 验收环境中的 `INTERNAL_SERVICE_TOKEN` 与 `CONFIG_REFRESH_TOKEN` 改为必须显式配置，不再提供固定弱默认值回退
- 已将 `.env.example`、联调脚本示例和内部接口文档改成占位值或环境变量引用，避免继续传播固定密钥

### 4. 答辩材料

- 已新增 `docs/microservice-acceptance-defense.md`，覆盖 Eureka、Config、Gateway、Feign、Resilience4j、内部鉴权和聚合层保留原因

### 5. 路由级测试

- 已将 `gateway-service` 的 `GatewayRouteRewriteIntegrationTest` 切到 `lb://service-id` 场景，验证 `/api/**`、`/elm/**`、`/services/address/**` 在负载均衡入口下的实际转发与改写行为

### 6. 第二梯队优化项

- 已移除主配置中的源码级固定 token fallback，统一改为显式环境变量注入；本地开发仅在 `application-local.properties` 中保留 local profile 兜底值
- 已将现存测试中的 `@MockBean` 切换为 `@MockitoBean`，避免继续积累 Spring Boot 3.5 弃用告警
- 已为 `points-service`、`account-service`、`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service` 补齐健康检查，并收紧 `food-service`、`user-service`、`elm-v2` 的启动依赖链

## 当前状态

所有已识别的微服务验收收口项均已落地。