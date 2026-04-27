# elm-cloud 微服务架构汇报文档（用于答辩/汇报）

> 适用范围：本文件仅描述本仓库中 `elm-cloud` 目录下的实现与运行方式。
>
> 背景对照：原单体后端为 `elm-v2.0`，原前端为 `elm-frontend`；历史上曾有过过渡拆分尝试（现已弃用），本汇报重点放在 **elm-cloud 的微服务拆分设计与迁移落地**。

## 1. 项目背景：从单体到微服务

### 1.1 单体（elm-v2.0）常见痛点

以典型外卖平台为例，单体模式会逐渐显现问题：

- **模块边界不清晰**：用户、订单、商品、商家、钱包、积分等业务耦合在一个进程/一个代码库的同一运行时中。
- **发布与回滚成本高**：任何一个功能改动都要重启整个应用；回滚也只能整体回滚。
- **扩展能力受限**：热点业务（如下单、查商品）无法独立扩容，只能整体扩容。
- **团队并行开发冲突**：多人同时修改同一工程的同一模块，冲突与联调成本上升。
- **稳定性风险扩大**：一个模块的慢 SQL / 线程阻塞可能拖垮整个应用。

### 1.2 微服务拆分目标

- 把系统按业务域拆分为 **多个独立部署单元**，每个服务围绕一个清晰的领域能力。
- 通过 Spring Cloud 体系提供：
  - **服务注册发现**（Eureka）
  - **统一入口与路由**（Gateway）
  - **客户端负载均衡**（LoadBalancer，课程语境常称 Ribbon）
  - **配置中心**（Config Server）
  -（可选）动态配置刷新（Bus）、熔断降级（Hystrix）等

> 重要说明（务实汇报口径）：
>
> - `elm-cloud` 已经实现并稳定运行的核心能力是：Eureka / Gateway / Config Server / 客户端负载均衡（Spring Cloud LoadBalancer）/ Docker Compose 一键启动。
> - `Bus 动态刷新`、`Hystrix` 这两项在 **当前 `elm-cloud` 代码与依赖中并未集成**（下文会说明可扩展方案），汇报时按“规划能力/可演进方向”来讲，避免与代码不一致。

## 2. 微服务怎么拆：拆分原则与边界设计（汇报重点）

### 2.1 拆分原则（从单体落地到服务边界）

我们把单体 `elm-v2.0` 按外卖平台的核心领域拆分，原则是：

- **围绕业务能力拆**：一个服务只做一类清晰的事（比如“订单”“用户”），避免“什么都在一个服务里”。
- **数据模型打平，避免跨服务 ORM 级联**：服务之间只传 ID / DTO，不做跨服务的 JPA 关联。
- **先拆核心链路，再拆支撑域**：优先保证“登录 → 浏览 → 加购 → 下单/支付”的链路可跑通。
- **接口先行（API）**：先定义对外 API（通过 Gateway 暴露），再迁移单体中对应 Controller/Service/DAO。
- **可独立部署**：每个服务都有自己的启动配置、Dockerfile，可由 compose 拉起。

### 2.2 拆分结果：服务与职责（结合代码与文档）

基础设施（微服务底座）：

- `eureka-server`：注册中心
- `config-server`：配置中心（native 示例）
- `gateway`：统一入口与路由
- `mysql`：数据库

业务服务（按领域能力拆分）：

- `user-service`：用户注册/登录/JWT 与用户信息（见 `docs/user-service.md`）
- `merchant-service`：商家/店铺/审核流程（见 `docs/merchant-service.md`）
- `product-service`：菜品/商品（见 `docs/product-service.md`）
- `cart-service`：购物车（见 `docs/cart-service.md`）
- `order-service`：订单创建/查询/状态流转（见 `docs/order-service.md`）
- `address-service`：地址
- `points-service`：积分
- `wallet-service`：钱包

### 2.3 拆分边界示例（用于解释“为什么这么拆”）

- 用户域：认证与用户基本资料是其他业务的基础能力，拆为 `user-service`。
- 交易域：下单/取消/订单查询是核心交易流程，拆为 `order-service`。
- 商品与商家域：商品与商家信息变动频率不同、访问模式不同，拆为 `product-service` 与 `merchant-service`。
- 资金与积分：属于风控敏感域，拆为 `wallet-service` 与 `points-service`，便于后续引入对账/补偿等机制。

## 3. 迁移过程怎么做：迁移步骤与难点（汇报重点）

### 3.1 迁移步骤（从单体到可运行微服务）

我们采用“可运行优先”的迁移方式：

1. **搭底座**：先把 `eureka-server`、`gateway`、`config-server` 跑起来，保证服务可注册、可路由。
2. **拆服务骨架**：为每个业务域创建独立 Spring Boot 应用（module + Dockerfile）。
3. **迁移单体代码**：按“Controller → Service → DAO/Entity”顺序搬迁与重构；在新服务内完成编译通过与冒烟。
4. **网关接入**：把对外 API 按路径挂到 Gateway（例如 `/elm/api/orders/**` → `order-service`）。
5. **容器化联调**：使用 `docker-compose.yml` 拉起全链路，反复跑通主链路。

### 3.2 迁移难点与对应解决方案（结合你们现有实现）

1) **单体里常见的 ORM 深度关联在微服务里不可用**

- 难点：单体模式下 `Entity` 之间经常有级联关系，一次查询拉出“订单+用户+商家+菜品”等深层对象。
- 解决：在微服务中将关系 **打平**（只保留 `*_id`），需要展示聚合数据时由前端/聚合层（或后续 BFF）组装。
  - 示例：`cart-service` 文档中明确了 `Cart` 仅保留 `user_id/business_id/food_id/quantity`。

2) **旧前端/旧接口兼容问题（表单提交 vs REST JSON）**

- 难点：单体时代的部分接口可能是表单提交、命名不规范。
- 解决：迁移时保留关键兼容端点，同时新增更 REST 的 `/elm/api/**` 接口给 Gateway 路由。
  - 示例：`order-service` 文档里提到同时兼容 `/OrdersController/**` 与新 `/api/orders/**`。

3) **鉴权从“单体过滤器”变成“跨服务认证信息传递”**

- 难点：单体里 JWT 校验在一个进程内完成；拆分后需要让多个服务正确识别用户身份。
- 解决：
  - 由 `user-service` 负责登录与 token 发行（见 `docs/user-service.md`）。
  - 服务间调用时把 `Authorization` 头透传给下游，保证权限一致。
    - 示例：`merchant-service` 的 `RestTemplateConfig` 增加拦截器，自动透传 `Authorization`。

4) **本地联调的“可复现环境”**

- 难点：单体时代本地跑起来简单，但微服务需要一堆基础设施（注册中心、网关、数据库、多个服务）。
- 解决：用 `elm-cloud/docker-compose.yml` 把全链路编排为“一条命令拉起”。

## 4. 微服务之间怎么通信与配合（汇报重点）

这一部分建议用“请求链路 + 服务间调用方式”来讲清楚：

### 4.1 通信方式一：外部流量统一走 Gateway

- 浏览器（前端）只访问：
  - `http://localhost`（前端）
  - `http://localhost:8080`（Gateway）
- Gateway 按路径转发到各业务服务（配置见 `gateway/src/main/resources/application.yml`）。

### 4.2 通信方式二：服务间调用使用“服务名寻址 + 客户端负载均衡”

- 每个服务注册到 Eureka，获得“服务名 → 实例列表”。
- 在需要调用其他服务时，用 `@LoadBalanced RestTemplate` 访问：
  - `http://user-service/...`、`http://order-service/...` 这类地址。

示例（真实代码位置）：

- `merchant-service` 调用 `user-service`：`http://user-service/elm/api/users/{id}`（见 `merchant-service/.../UserService.java`）。

### 4.3 认证信息的协作：Authorization 透传

- 外部请求进来，携带 `Authorization: Bearer <token>`。
- 业务服务之间互相调用时，把 Authorization 头透传下去。
  - 示例：`merchant-service/.../RestTemplateConfig.java` 中通过 RestTemplate interceptor 透传请求头。

### 4.4 典型业务链路（可直接画在 PPT 上）

以“用户下单”为例（概念级）：

1. 用户登录：前端 → Gateway → `user-service`，获取 JWT
2. 查询菜品：前端 → Gateway → `product-service`
3. 加入购物车：前端 → Gateway → `cart-service`
4. 创建订单：前端 → Gateway → `order-service`
5. 支付/钱包：前端 → Gateway → `wallet-service`（后续可扩展为订单与钱包的事务补偿）

> 这条链路的核心价值：每个服务只对自己的领域负责，通信通过 Gateway + Eureka + LoadBalancer 串起来。

## 5. Eureka：服务注册与发现

### 5.1 解决的问题

- 服务实例在 Docker 环境中 IP 动态变化，不能写死地址。
- 需要根据 **服务名** 找到 **可用实例列表**。

### 5.2 在 elm-cloud 中如何落地

- `eureka-server` 模块依赖：`spring-cloud-starter-netflix-eureka-server`（见 `eureka-server/pom.xml`）。
- 服务端配置：`eureka-server/src/main/resources/application.yml`
  - `register-with-eureka: false`、`fetch-registry: false`（Server 本身不作为 Client）。
- 业务服务作为 Eureka Client：依赖 `spring-cloud-starter-netflix-eureka-client`（各服务 pom 中可见）。

### 5.3 运行与验证

- 打开 Eureka 控制台：http://localhost:8761
- 你应当能看到 `gateway`、`user-service` 等实例注册。

## 6. Gateway：统一入口、路由与后端解耦

### 6.1 解决的问题

- 前端不需要知道每个微服务的地址与端口。
- 统一处理横切能力（认证、限流、日志等——本项目主要用于路由与统一入口）。

### 6.2 在 elm-cloud 中如何落地

- `gateway` 依赖：
  - `spring-cloud-starter-gateway`
  - `spring-cloud-starter-netflix-eureka-client`
  - `spring-cloud-starter-loadbalancer`
  - `spring-boot-starter-actuator`
  （见 `gateway/pom.xml`）

- 路由配置位置：`gateway/src/main/resources/application.yml`
  - 使用 `lb://<service-name>` 作为 `uri`
  - 典型示例：
    - `lb://user-service` 处理鉴权与用户相关路径
    - `lb://order-service` 处理订单相关路径

> 汇报中的一句话解释：
>
> “Gateway 通过 Eureka 获取服务实例列表，然后把 `/elm/api/**` 的请求按路由规则转发到对应的业务服务，从而实现前后端解耦。”

## 7. Ribbon / 负载均衡：课程语境 vs 现状实现

### 7.1 课程里常说的 Ribbon

Netflix Ribbon 是早期 Spring Cloud Netflix 体系的客户端负载均衡方案，典型用法是：

- `@LoadBalanced RestTemplate`
- 使用 `http://<service-name>/...` 发起请求，由 Ribbon 选择一个实例并替换真实地址

### 7.2 elm-cloud 实际使用的是 Spring Cloud LoadBalancer

在 Spring Cloud 2023.x（对应 Spring Boot 3.x）中：

- Ribbon 已经退出主流依赖链。
- 官方推荐使用 **Spring Cloud LoadBalancer** 作为“客户端负载均衡”实现。

`elm-cloud` 中落地体现：

- `gateway` 显式依赖：`spring-cloud-starter-loadbalancer`（见 `gateway/pom.xml`）。
- 多个业务服务中存在 `@LoadBalanced RestTemplate` Bean（例如：
  - `merchant-service/src/main/java/.../RestTemplateConfig.java`
  - `order-service/src/main/java/.../RestTemplateConfig.java`
  - `cart-service/src/main/java/.../RestTemplateConfig.java`
  ）

并且服务调用使用服务名：

- `merchant-service` 内部调用用户服务：`http://user-service/elm/api/users/{id}`（见 `merchant-service/.../UserService.java`）。

> 汇报建议说法：
>
> “课程里讲的 Ribbon 能力，在我们的 Spring Cloud 新版本中由 Spring Cloud LoadBalancer 承担，调用方式保持一致（@LoadBalanced + 服务名），但底层实现更现代、维护更活跃。”

## 8. Config 配置中心：集中化配置管理

### 8.1 解决的问题

- 多服务配置分散、难以统一管理。
- 环境切换（dev/test/prod）配置重复。

### 8.2 在 elm-cloud 中如何落地

- `config-server` 依赖：`spring-cloud-config-server`（见 `config-server/pom.xml`）。
- Config Server 默认以 `native` 模式运行，并支持切换到 `git` 模式：
  - 配置文件：`config-server/src/main/resources/application.yml`
  - `search-locations: file:./config,classpath:/config`
- 当前外部配置目录：`elm-cloud/config/`
- 当前已接入 Config Client 的服务：`gateway`、全部主要业务服务
- 当前客户端通过 `bootstrap.yml` 中的 discovery-first 配置发现 `config-server`
- 当前 compose 已部署 `config-server-1` 与 `config-server-2` 两个实例

> 说明：当前 `elm-cloud` 已经具备 Config Server 双实例和 discovery-first 这两个更接近课件标准的特征，但默认仍采用更适合本地 Docker 演示的 native 配置源；如果需要，也可以切到 Git 模式。

## 9. Bus 动态刷新：现状与可扩展方案

### 9.1 Bus 是什么（用于汇报解释）

Spring Cloud Bus 用消息中间件（常见：RabbitMQ/Kafka）把“配置变更事件”广播到所有服务实例。

典型效果：

- 配置中心更新后，通过 `POST /actuator/bus-refresh` 让集群各节点自动刷新配置。

### 9.2 elm-cloud 当前现状（与代码一致）

当前 `elm-cloud` 已经落地 Bus：

- `docker-compose.yml` 中已经部署了 RabbitMQ。
- `config-server`、`gateway` 和主要业务服务已引入 `spring-cloud-starter-bus-amqp`。
- `config-server` 已暴露 `bus-refresh`、`bus-env` 端点。
- 接入服务已暴露 `bus-refresh` / `refresh` 相关端点。

因此：**Bus 的基础设施链路已经落地启用，并已覆盖主要服务。**

同时也要注意两点：

- 当前已经在 `order-service` 中补了 `@RefreshScope` 演示接口，可用于现场验证热刷新。
- 但默认配置源仍然是 native 挂载目录，Git 配置仓库仍需接入实际仓库地址。

### 9.3 如果要在 elm-cloud 上落地（演进建议）

- 如果要回到更完整的课件标准方案，可将默认运行方式切到 Git 模式，并为 Git 仓库补上分支与同步策略。
- 如果要进一步增强演示效果，可以在更多服务中补 `@RefreshScope` 配置端点。

（这一段适合放在“未来工作/可扩展点”，但可以明确说明：当前项目已经完成 Bus 基础设施接入，不再是“完全未实现”的状态。）

## 10. Hystrix 熔断降级：现状与替代方案

### 10.1 Hystrix 是什么

Hystrix 是 Netflix OSS 的熔断/隔离/降级库，解决：

- 下游服务超时或异常导致上游线程耗尽
- 通过熔断与降级保护系统整体可用性

### 10.2 elm-cloud 当前现状（与代码一致）

通过依赖与代码检索，当前 `elm-cloud`：

- **没有** `spring-cloud-starter-netflix-hystrix` 依赖。
- **没有** `@EnableHystrix` / `@EnableCircuitBreaker` 等启用代码。

因此：**Hystrix 在当前 elm-cloud 中未落地**。

### 10.3 推荐的现代替代：Resilience4j

在 Spring Boot 3 / Spring Cloud 2023 的栈中，更推荐：

- Resilience4j（Circuit Breaker / Bulkhead / RateLimiter / Retry / TimeLimiter）

汇报建议口径：

- “课程中讲的 Hystrix 熔断思想我们保留，但在新版技术栈中会用 Resilience4j 来实现同等能力。”

## 11. Docker Compose：一键启动与可复现环境

### 11.1 为什么用 compose

- 把数据库、注册中心、配置中心、网关、业务服务、前端拉到同一网络。
- 减少本地安装依赖，保障“可复现”。

### 11.2 本项目的启动顺序与端口

- `mysql`：3306
- `eureka-server`：8761
- `config-server`：8888
- `gateway`：8080
- `frontend`：80

业务服务不暴露宿主机端口，通过 `gateway` 对外提供 API。

## 12. 建议你汇报时的结构（可直接照念）

1. 单体痛点（发布、扩展、稳定性、协作）
2. 怎么拆（拆分原则 + 服务边界）
3. 怎么迁（迁移步骤 + 难点与解决方案）
4. 怎么协作（通信方式 + 典型业务链路）
5. Eureka / Gateway / 负载均衡 / Config（底座落地）
6. Bus、Hystrix（可演进能力：现状未落地，给出方案；Hystrix 推荐 Resilience4j）
7. Compose（一键启动与可复现联调环境）

---

## 附：快速指路

- 总览结构：`docs/project-structure.md`
- 运行指南：`run.md`
- 排错手册：`docs/troubleshooting.md`
- 路由配置：`gateway/src/main/resources/application.yml`
- Config Server 配置仓库示例：`config-server/src/main/resources/config/application.yml`
