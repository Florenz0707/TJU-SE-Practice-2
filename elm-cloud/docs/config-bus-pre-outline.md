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

> 课件里通常是远程 Git 仓库 + Config Server 集群 + 所有服务全量接入。我这里做了几个调整。第一，我没有用远程 Git，而是使用了 Config Server 的 native 模式，直接读取 Docker 部署目录下的外部配置文件；第二，我当前只保留了一个 Config Server 实例，没有继续扩成高可用集群；第三，我不是一次性让所有服务都接入配置中心，而是先让 gateway、order-service、user-service 三个服务接入；第四，Bus 也是先在 config-server、order-service、user-service 这条链路上接通。

### 2.4 为什么这么改

可以讲：

> 这样改主要是出于两个考虑。第一个是本地演示和联调稳定性，native 模式不依赖外网 Git，也不需要账号和 webhook；第二个是当前项目本身还在持续调整，如果一开始就强行让所有服务全部迁移到配置中心，排查问题会更复杂，所以我采用的是“先把链路搭通，再逐步扩大覆盖面”的做法。

### 2.5 当前完成度如何评价

推荐讲法：

> 如果按课件原版标准打分，这部分不是 100% 全做满，但核心链路已经搭起来了：Config Server 是真实可运行的，RabbitMQ 和 Bus 也已经接入，刷新端点已经开放，部分服务已经通过配置中心启动。当前还没有做满的地方是 Config Server 集群、全量服务接入，以及显式的 `@RefreshScope` 动态刷新演示 Bean。

## 3. 代码层面的证据怎么讲

### 3.1 Config Server 已经落地

你可以说：

> `config-server` 模块主启动类上有 `@EnableConfigServer`，依赖中引入了 `spring-cloud-config-server`，并且在配置里启用了 native 模式。这说明配置中心不是概念图，而是已经写进代码并能启动的真实服务。

### 3.2 配置源是外挂目录，不是 Git

你可以说：

> 当前配置文件不在远程 Git，而是在 `elm-cloud/config/` 目录里。Docker Compose 启动时会把这个目录挂载到 Config Server 容器中，所以配置是外部化的，可以脱离 Jar 独立管理。

### 3.3 Config Client 接入范围

你可以说：

> 当前 `gateway`、`order-service`、`user-service` 这三个服务引入了 Config Client，并且在 `bootstrap.yml` 中通过 `http://config-server:8888` 拉配置。这里我没有使用 discovery-first，而是直接写死服务地址，因为在 Docker Compose 内部网络里这种方式更直接、也更稳定。

### 3.4 Bus 的证据

你可以说：

> `config-server`、`order-service`、`user-service` 都引入了 `spring-cloud-starter-bus-amqp`，Docker Compose 里也部署了 RabbitMQ，同时开放了 `bus-refresh` 相关 Actuator 端点，所以 Bus 链路是已经接上的。

## 4. 你在台上最稳的“结论句”

推荐直接背下来：

> 所以我这部分工作的结果，不是完全照课件做出来的标准版 Config + Bus，而是一个基于当前项目实际情况的替代实现。它保留了配置中心、外部化配置、消息总线广播刷新这些核心思想，并且已经在部分服务上跑通；但高可用集群、全量服务迁移和完整热刷新演示点还属于后续可继续完善的部分。

## 5. 如果老师让你现场演示，建议怎么演

### 5.1 最适合展示的内容

现场演示建议不要把目标定成“我一定要证明某个业务字段立刻热更新”，因为当前代码里没有专门的 `@RefreshScope` 演示 Bean，这样一旦老师盯着可见结果问，很容易被动。

更稳的演示顺序是：

1. 展示 `docker-compose.yml` 里确实有 `rabbitmq` 和 `config-server`
2. 展示 `config-server` 的 native 配置和外挂 `./config` 目录
3. 展示 `order-service` / `user-service` 的 `bootstrap.yml` 和 Bus 依赖
4. 展示 `POST /actuator/bus-refresh` 这个刷新入口
5. 最后主动说明：当前重点是把基础设施链路接通，业务热更新演示 Bean 还可以继续补强

### 5.2 如果老师坚持问“那你怎么证明刷新了”

你可以回答：

> 当前仓库里没有专门暴露一个 `@RefreshScope` 的测试接口，所以我更适合证明的是刷新链路和依赖接入已经完成，而不是强行说所有业务配置都能在界面上立刻看到变化。这个部分如果继续完善，我会补一个安全的演示型配置接口来展示热更新效果。

## 6. 3 分钟版汇报稿

下面这段可以直接按自然语速讲：

> 我负责的是微服务拆分后的 Config + Bus 配置部分。这个部分我没有完全照搬课件里的 Git 配置仓库和 Config 集群方案，而是结合我们当前项目的技术栈和 Docker Compose 环境，做了一个更容易跑通的替代实现。现在项目里已经有独立的 Config Server，主启动类开启了 `@EnableConfigServer`，配置文件使用 native 模式，从 `elm-cloud/config/` 这个外部目录统一读取。这样配置就从服务内部抽离出来了。客户端这边，我先让 gateway、order-service、user-service 三个服务接入了 Config Client，它们启动时会从 `config-server:8888` 拉取配置。为了避免配置中心挂掉就导致服务起不来，我还保留了 `fail-fast: false` 的兜底策略。Bus 这一块，我接入了 RabbitMQ，并在 config-server、order-service、user-service 里加入了 Spring Cloud Bus 的 AMQP 依赖，也开放了 `bus-refresh` 端点。因此这部分的基础设施链路是已经打通的。当前没有完全做满的地方主要有三个：第一，没有使用远程 Git；第二，没有做 Config Server 集群；第三，代码里暂时没有专门的 `@RefreshScope` 演示 Bean，所以更适合把这部分定义为“核心链路已经跑通，但完整覆盖和热刷新展示还可以继续加强”。

## 7. 你最后收口时可以这样说

> 如果老师更看重“是否完全按课件实现”，那我这部分属于替代实现，不是逐项照搬；但如果看“配置中心和总线的核心思想是否已经落地到代码和部署链路中”，那这部分已经落地，并且可以继续扩展到全部服务和高可用集群。