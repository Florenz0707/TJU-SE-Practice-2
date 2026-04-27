# Config + Bus 汇报提纲（按当前代码版本）

## 1. 适合你的开场口径

可以直接这样讲：

> 我负责的是微服务拆分后的 Config + Bus 配置部分。这个部分我没有完全照搬课件里的标准实现，而是结合我们项目当前的 Spring Boot 3.3、Spring Cloud 2023 和 Docker Compose 运行环境，做了一个可运行的替代方案。核心目标是三件事：第一，把一部分服务配置从服务本地抽出去，由 Config Server 统一提供；第二，引入 RabbitMQ 和 Spring Cloud Bus，建立配置刷新广播链路；第三，保留本地运行时的稳定性和兜底能力。

这段话的好处是：

- 先主动承认“和课件不完全一样”
- 再强调“我不是没做，而是做了替代实现”
- 最后把老师注意力引到你的工程取舍上

## 2. 你可以按下面的 5 个点展开

### 2.1 我解决了什么问题

可以讲：

> 单体阶段每个模块都有自己的本地配置，拆成微服务后，如果每个服务都单独维护配置，修改和排查会变得更麻烦。所以我做这部分时，主要想解决两个问题：一个是把配置从服务内部抽离出来，另一个是让配置变更之后不一定要逐个重启服务。

### 2.2 我最终采用的技术方案

可以讲：

> 我最终使用的是 Spring Cloud Config Server + RabbitMQ + Spring Cloud Bus。Config Server 负责统一对外提供配置，RabbitMQ 作为 Bus 的消息中间件，Bus 负责把配置刷新事件广播到接入的服务实例。

### 2.3 和课件标准方案相比，我改了什么

这一段最关键，建议照着讲：

> 课件里通常是远程 Git 仓库 + Config Server 集群 + 所有服务全量接入。我这里做了两个层次的实现。默认层是更稳的 native 模式，直接读取 Docker 部署目录下的外部配置文件；增强层是我已经把 Config Server 扩成了双实例，并把客户端改成通过 Eureka discovery-first 发现配置中心。这样默认演示更稳，同时又保留了切到 Git 模式、贴近课件标准的能力。

### 2.4 为什么这么改

可以讲：

> 这样改主要是出于两个考虑。第一个是本地演示和联调稳定性，native 模式不依赖外网 Git，也不需要账号和 webhook；第二个是当前项目本身还在持续调整，如果一开始就强行让所有服务全部迁移到配置中心，排查问题会更复杂，所以我采用的是“先把链路搭通，再逐步扩大覆盖面”的做法。

### 2.5 当前完成度如何评价

推荐讲法：

> 如果按课件原版标准打分，这部分现在已经更接近标准版了：Config Server 是双实例运行的，客户端通过 Eureka 发现配置中心，RabbitMQ 和 Bus 已经接入，gateway 和主要业务服务都已经通过配置中心启动，而且我还补了显式的 `@RefreshScope` 动态刷新演示 Bean。当前还没有做满的地方主要是 Git 仓库本身的实际接入和版本化管理流程。

## 3. 代码层面的证据怎么讲

### 3.1 Config Server 已经落地

你可以说：

> `config-server` 模块主启动类上有 `@EnableConfigServer`，依赖中引入了 `spring-cloud-config-server`，并且在配置里启用了 native 模式。这说明配置中心不是概念图，而是已经写进代码并能启动的真实服务。

### 3.2 配置源是外挂目录，不是 Git

你可以说：

> 当前配置文件不在远程 Git，而是在 `elm-cloud/config/` 目录里。Docker Compose 启动时会把这个目录挂载到 Config Server 容器中，所以配置是外部化的，可以脱离 Jar 独立管理。

### 3.3 Config Client 接入范围

你可以说：

> 当前 `gateway` 和主要业务服务都引入了 Config Client，并且在 `bootstrap.yml` 中改成了 discovery-first，通过 Eureka 发现名为 `config-server` 的配置中心服务。这样即使有两个 Config Server 实例，客户端也不需要感知具体节点地址。

### 3.4 Bus 的证据

你可以说：

> `config-server`、`gateway` 和主要业务服务都引入了 `spring-cloud-starter-bus-amqp`，Docker Compose 里也部署了 RabbitMQ，同时开放了 `bus-refresh` 相关 Actuator 端点，所以 Bus 链路是已经接上的。

## 4. 你在台上最稳的“结论句”

推荐直接背下来：

> 所以我这部分工作的结果，是一个更接近课件标准、但仍然保留工程化兜底的实现。它保留了配置中心、外部化配置、消息总线广播刷新这些核心思想，并且已经具备双 Config Server、discovery-first 和可现场演示的热刷新接口；默认仍保留 native 模式，Git 模式也已经预留好了切换入口。

## 5. 如果老师让你现场演示，建议怎么演

### 5.1 最适合展示的内容

更稳的演示顺序是：

1. 展示 `docker-compose.yml` 里确实有 `config-server-1` 和 `config-server-2`
2. 展示客户端 `bootstrap.yml` 已改成 discovery-first，而不是直连固定地址
3. 展示 `config-server` 默认是 native 模式，但支持 `CONFIG_SERVER_MODE=git`
4. 访问 `GET /elm/api/orders/runtime-config` 看当前配置值
5. 修改配置文件，或者在 git 模式下提交配置仓库变更
6. 向 `http://localhost:8888/actuator/bus-refresh` 或 `http://localhost:8889/actuator/bus-refresh` 发请求
7. 再次访问 `runtime-config`，展示值已经更新

### 5.2 如果老师坚持问“那你怎么证明刷新了”

你可以回答：

> 我现在已经补了一个安全的演示型配置接口，就是 `order-service` 的 `/api/orders/runtime-config`。它专门用于证明配置值可以在 Bus 刷新后发生变化，不影响现有业务逻辑。

## 6. 3 分钟版汇报稿

下面这段可以直接按自然语速讲：

> 我负责的是微服务拆分后的 Config + Bus 配置部分。这个部分我没有机械地照搬课件，而是做成了一个更接近课件标准、同时又兼顾本地演示稳定性的实现。现在项目里有两个 Config Server 实例，都会注册到 Eureka，客户端通过 discovery-first 发现名为 `config-server` 的服务，而不是写死某个地址。默认情况下，Config Server 使用 native 模式，从 `elm-cloud/config/` 这个外部目录统一读取配置；如果需要更贴近课件标准，也可以通过环境变量切到 Git 模式。Bus 这一块，我接入了 RabbitMQ，并把 gateway 和主要业务服务都加入了 Spring Cloud Bus 的 AMQP 依赖，也开放了 `bus-refresh` 端点。另外我还在 order-service 里补了一个 `@RefreshScope` 演示接口 `/api/orders/runtime-config`，可以直接证明配置值在刷新后会变化。因此这部分的基础设施链路、集群化入口和演示链路都已经打通。当前还没有完全做满的地方主要是 Git 仓库本身的实际接入和版本化管理流程。

## 7. 你最后收口时可以这样说

> 如果老师更看重“是否完全按课件实现”，那我这部分属于替代实现，不是逐项照搬；但如果看“配置中心和总线的核心思想是否已经落地到代码和部署链路中”，那这部分已经落地，并且可以继续扩展到全部服务和高可用集群。