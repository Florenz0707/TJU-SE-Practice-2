# 微服务验收答辩提纲

## 1. 整体架构怎么回答

- 对外统一入口是 `elm-v2.0`，负责兼容原课程接口、JWT 鉴权和跨服务编排。
- 对内已拆成 `business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service`，并额外扩展了 `account-service`、`points-service`。
- 云治理层由 `config-server`、`discovery-server`、`gateway-service` 提供配置中心、注册发现和统一网关能力。

## 2. Eureka 怎么回答

- 使用双节点注册中心：`discovery-server-a` 和 `discovery-server-b`。
- 商家、菜品、购物车、订单四个核心业务服务按双实例部署，能在 Eureka 中看到同一 `serviceId` 下多实例注册。
- Gateway 和各客户端都通过服务名访问，而不是在验收部署中写死容器地址。

## 3. Config Server 怎么回答

- 使用 Spring Cloud Config Server 统一托管服务配置。
- 当前采用 `native` 配置仓模式，本质上仍然是集中配置管理，只是配置源挂载为本地目录而不是远程 Git。
- 这样做的好处是课程验收环境更稳定、离线可演示、启动链更简单；能力上仍保留集中下发和刷新入口。

## 4. Gateway 怎么回答

- 外部流量优先走 `gateway-service`。
- `/api/**` 转发到聚合层 `elm`，`/services/*/**` 支持直通各微服务以便演示拆分后的真实服务入口。
- 当前 compose 验收环境下，上游地址统一使用 `lb://service-id`，说明网关本身已经接入服务发现和负载均衡，而不是简单反向代理。

## 5. Feign 和熔断怎么回答

- `food-service` 调 `business-service` 已使用 OpenFeign。
- 熔断降级没有继续使用 Hystrix，而是使用当前 Spring Cloud 体系推荐的 Resilience4j。
- 这属于技术栈等价升级，不影响“服务调用 + 容错治理”的课程目标，反而更符合当前生态。

## 6. 内部鉴权怎么回答

- 微服务内部接口统一要求 `X-Internal-Service-Token`。
- `/api/inner/**` 路径会经过内部 token 过滤器校验，未携带或不匹配直接返回 `401`。
- 当前 compose 验收环境已去掉固定弱默认值回退，`INTERNAL_SERVICE_TOKEN` 和 `CONFIG_REFRESH_TOKEN` 必须在 `.env` 中显式配置。

## 7. 为什么保留 elm-v2.0 聚合层

- 课程原始接口和前端调用路径较多，直接全部改成前端对多个微服务逐一调用，回归成本和演示复杂度都很高。
- `elm-v2.0` 作为聚合层，可以保留对外接口稳定性，同时把真实业务能力逐步下沉到微服务。
- 这也是实际工程里常见的渐进式拆分方式，不是“假微服务”，因为核心域数据与内部接口已经拆开。

## 8. 现场如果老师问“你们真的考虑过稳定性吗”

- Compose 启动链已为 `config-server`、`discovery-server`、`elm-v2`、`gateway-service` 增加健康检查。
- 关键依赖已从 `service_started` 收紧为 `service_healthy`，降低了冷启动竞态。
- 前端现在也等待网关健康后再启动，现场一键演示更稳定。

## 9. 现场如果老师问“配置刷新和运维怎么做”

- Gateway 提供统一配置刷新入口，用 token 保护。
- 已有配置刷新结果聚合测试，能够区分成功实例和失败实例；如果注册发现本身异常，网关会返回结构化 `503`，不是直接抛出不可解释的 500。
- 如果个别实例刷新失败，可以按实例重试或人工重启对应服务，不影响整体架构说明。

## 10. 一句话总结

- 这个项目不是只把单体拆成多个端口，而是已经补齐了注册中心、配置中心、网关、服务发现、负载均衡、内部鉴权和服务容错，达到了课程验收会重点看的“微服务高层能力”。