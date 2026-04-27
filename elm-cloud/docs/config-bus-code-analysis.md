# Config + Bus 当前代码实现分析

## 1. 文档目的

这份文档只基于当前 `elm-cloud` 仓库中的最终代码版本，总结配置中心和动态刷新的实际落地情况，供 pre 和答辩统一口径使用。

## 2. 一句话结论

当前项目已经完成一套可运行、可验证的 Config + Bus 实现：

- Config Server 已经独立成服务，并部署为双实例
- 客户端通过 Eureka discovery-first 发现 `config-server`
- 配置统一集中在 `elm-cloud/config/` 目录下
- RabbitMQ 和 Spring Cloud Bus 已经接通
- `gateway` 与主要业务服务都已接入 Config Client 和 Bus
- 所有微服务 Controller 都已加上 `@RefreshScope`
- `order-service` 提供了 `/elm/api/orders/runtime-config` 用于直接证明热刷新结果

## 3. 当前实现包含哪些核心能力

### 3.1 配置中心已经独立运行

当前 `config-server` 不是概念模块，而是实际运行中的基础设施服务：

- 主启动类启用了 `@EnableConfigServer`
- 服务端依赖包含 `spring-cloud-config-server`
- compose 中启动了 `config-server-1` 和 `config-server-2`
- 两个实例都会注册到 Eureka

这说明当前系统已经具备独立配置中心，而不是每个服务各自维护配置。

### 3.2 配置已经中心化管理

当前所有中心化配置统一放在 `elm-cloud/config/` 目录下，包括：

- `application.yml`
- `gateway.yml`
- `order-service.yml`
- `user-service.yml`
- `merchant-service.properties`
- `product-service.properties`
- `cart-service.properties`
- `address-service.properties`
- `points-service.properties`
- `wallet-service.properties`

这些文件由 Config Server 统一读取，再分发给对应客户端服务。

### 3.3 客户端已经改成 discovery-first

当前 `gateway` 和主要业务服务都在 `bootstrap.yml` 中启用了：

- `spring.cloud.config.discovery.enabled=true`
- `spring.cloud.config.discovery.service-id=config-server`

这意味着客户端不再依赖固定地址，而是通过 Eureka 发现配置中心实例，更适合双实例和后续扩容场景。

### 3.4 Bus 链路已经真正接通

当前 Bus 不是“准备接入”，而是已经进入工程实现：

- `config-server`、`gateway` 和主要业务服务都引入了 `spring-cloud-starter-bus-amqp`
- `docker-compose.yml` 中部署了 RabbitMQ
- Config Server 暴露了 `busrefresh` 与 `bus-env` 端点
- 自动验收和演示脚本已经能通过 Bus 完成配置广播刷新

所以这条链路已经具备“依赖存在 + 中间件存在 + 端点存在 + 实际运行成功”四层证据。

### 3.5 动态刷新已经有业务侧证明

当前项目里，动态刷新不是只停留在基础设施层：

- 所有微服务 Controller 都已经统一加上 `@RefreshScope`
- `order-service` 中有 `RefreshableDemoProperties`
- `order-service` 暴露了 `GET /elm/api/orders/runtime-config`

因此现在可以直接修改 `elm-cloud/config/order-service.yml` 中的演示字段，再调用 `POST /actuator/busrefresh`，最后通过运行时接口观察返回值变化。

## 4. 当前刷新链路如何工作

当前完整链路可以概括为 6 步：

1. 修改 `elm-cloud/config/` 中目标服务的配置文件
2. Config Server 读取中心化配置目录中的最新内容
3. 调用 `POST /actuator/busrefresh`
4. Config Server 将刷新事件发送到 RabbitMQ
5. 接入 Bus 的客户端服务接收事件并刷新上下文
6. 访问业务接口，观察配置值在运行时发生变化

这条链路已经通过脚本和接口完成过实际验证。

## 5. 为什么加了 `@RefreshScope` 还需要中心化配置文件

这是答辩里最容易被追问的点，可以直接这样理解：

- Config Server 解决的是“配置统一存放、统一下发”的问题
- `@RefreshScope` 解决的是“Bean 在运行时能不能重新读取配置”的问题

所以两者不是替代关系，而是配合关系。

更准确地说：

> 如果没有中心化配置文件，服务就没有统一的配置源；如果没有 `@RefreshScope`，很多 Bean 即使收到了刷新事件，也不会表现出运行时值变化。

当前项目里这两部分都已经具备：

- 中心化配置文件在 `elm-cloud/config/`
- 刷新作用域通过 Controller 级 `@RefreshScope` 和演示 Bean 落地

## 6. 当前设计的工程取舍

当前项目保留了 `fail-fast: false` 和本地兜底配置，这个取舍的意义是：

- 提高联调和答辩环境的启动成功率
- 避免基础设施短时不可用时整个系统完全起不来
- 让配置中心不可用时，服务仍能用本地配置降级启动

所以当前方案是“优先保证课程项目环境稳定”的工程化实现。

## 7. 答辩时最稳的口径

推荐直接使用下面这段：

> 当前项目已经把 Config + Bus 这条主链路真正落地了。现在有双 Config Server，通过 Eureka discovery-first 提供统一配置入口；配置集中放在 `elm-cloud/config/` 目录下；RabbitMQ 和 Spring Cloud Bus 已经接通；主要服务都能接收配置刷新事件；所有 Controller 都已经进入刷新作用域；同时 order-service 还提供了运行时接口用来直接证明刷新结果。因此，这部分不是停留在设计说明，而是已经进入了代码、部署和演示链路的最终实现。
