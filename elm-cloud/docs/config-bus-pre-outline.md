# Config + Bus 汇报提纲（最终代码版本）

## 1. 开场口径

可以直接这样讲：

> 我负责的是微服务拆分后的 Config + Bus 配置部分。当前实现已经把配置中心、配置广播刷新和运行时演示链路完整接通了。现在项目里有双 Config Server、Eureka discovery-first、RabbitMQ 总线广播、集中配置目录，以及 Controller 级别的 `@RefreshScope` 刷新能力，所以这部分既能跑通，也能现场证明效果。

## 2. 你要讲清楚的 5 个点

### 2.1 我解决了什么问题

可以讲：

> 微服务拆出来之后，最大的问题就是配置分散、修改麻烦，而且改完之后常常要逐个重启服务。我做这部分的核心目标，就是把配置统一收口到 Config Server，并让配置修改后可以通过 Bus 广播刷新到各个服务实例。

### 2.2 我最终采用的技术方案

可以讲：

> 我最终采用的是 Spring Cloud Config Server + RabbitMQ + Spring Cloud Bus。Config Server 负责集中提供配置，RabbitMQ 负责承载总线消息，Bus 负责把刷新事件广播给接入的服务实例。

### 2.3 当前代码里已经落地了什么

可以讲：

> 当前 `docker-compose.yml` 中已经有两个 Config Server 实例，都会注册到 Eureka；客户端通过 discovery-first 发现 `config-server`；配置统一放在 `elm-cloud/config/` 目录下；`gateway` 和主要业务服务都已经接入 Config Client 和 Bus；所有 Controller 都补上了 `@RefreshScope`；另外还有 `order-service` 的 `/elm/api/orders/runtime-config` 用于现场证明刷新结果。

### 2.4 为什么这样设计

可以讲：

> 这样设计主要是为了兼顾两点。第一，配置要集中管理，不能再散落在每个服务里；第二，演示必须稳定，所以我保留了本地兜底配置和 `fail-fast: false`，让环境在联调和答辩时更容易起得来、查得清楚。

### 2.5 当前完成度怎么评价

推荐讲法：

> 从当前代码来看，这部分已经不是“规划中能力”，而是已经真正落地的能力。双 Config Server、Eureka 发现、Bus 广播、Controller 级刷新作用域和运行时演示接口都已经可运行、可验证。

## 3. 代码证据怎么讲

### 3.1 Config Server 已经落地

你可以说：

> `config-server` 模块主启动类上有 `@EnableConfigServer`，compose 中会启动两个实例，说明配置中心不是概念图，而是实际运行中的基础设施服务。

### 3.2 配置已经集中管理

你可以说：

> 当前配置统一放在 `elm-cloud/config/` 目录里，由 Config Server 对外提供。服务自身通过 `bootstrap.yml` 在启动早期拉取配置，这就是集中配置管理。

### 3.3 客户端已经改成 discovery-first

你可以说：

> 当前 `gateway` 和主要业务服务在 `bootstrap.yml` 中都启用了 `spring.cloud.config.discovery.enabled=true`，并通过 `service-id=config-server` 去发现配置中心，所以客户端不依赖固定节点地址。

### 3.4 Bus 已经接通

你可以说：

> `config-server`、`gateway` 和主要业务服务都引入了 `spring-cloud-starter-bus-amqp`，部署里也有 RabbitMQ，Config Server 还暴露了 `busrefresh` 入口，所以这条广播链路已经实际可用。

### 3.5 动态刷新已经可证明

你可以说：

> 现在不仅有 `order-service` 的演示 Bean，所有微服务 Controller 也都已经统一加了 `@RefreshScope`。所以从代码结构和演示结果两方面，都可以证明动态刷新能力已经落地。

## 4. 台上最稳的结论句

推荐直接背下来：

> 所以我这部分工作的结果，是一个已经完整落地到代码、部署和演示链路里的 Config + Bus 实现。它具备双 Config Server、discovery-first、集中配置、总线广播刷新、Controller 级刷新作用域和运行时演示接口，能够现场证明配置修改后的刷新效果。

## 5. 现场演示顺序

建议按这个顺序讲：

1. 展示 `docker-compose.yml` 里有 `config-server-1` 和 `config-server-2`
2. 展示 `bootstrap.yml` 里启用了 discovery-first
3. 展示 `elm-cloud/config/` 是集中配置目录
4. 访问 `GET /elm/api/orders/runtime-config` 看基线值
5. 修改 `elm-cloud/config/order-service.yml` 中的演示字段
6. 调用 `POST /actuator/busrefresh`
7. 再次访问 `runtime-config`，展示返回值变化

## 6. 3 分钟版汇报稿

下面这段可以直接按自然语速讲：

> 我负责的是微服务拆分后的 Config + Bus 配置部分。现在项目里已经有两个 Config Server 实例，都会注册到 Eureka，客户端通过 discovery-first 发现统一的 `config-server` 服务名。所有需要集中管理的配置统一放在 `elm-cloud/config/` 目录下，由 Config Server 对外提供。Bus 这一块，我接入了 RabbitMQ，并把 `config-server`、`gateway` 和主要业务服务都加入了 Spring Cloud Bus 的 AMQP 依赖，这样配置变更后可以通过总线广播刷新。另外，为了让动态刷新不仅停留在基础设施层，我给所有微服务 Controller 都补上了 `@RefreshScope`，并在 order-service 里提供了 `/elm/api/orders/runtime-config` 演示接口。这样我就可以现场修改配置、触发 `busrefresh`、再直接看到接口返回值变化。因此，这部分已经形成了一条从代码、部署到演示都完整闭环的实现链路。

## 7. 最后收口

> 如果老师关注的是“有没有真正做出来”，那这部分已经做出来了；如果老师关注的是“能不能现场证明”，那我也可以通过修改中心化配置、调用 `busrefresh` 和查看运行时接口返回值来直接证明。
