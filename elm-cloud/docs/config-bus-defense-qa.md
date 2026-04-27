# Config + Bus 答辩问答清单

## 1. 你到底做了什么？

推荐回答：

> 我主要完成了五件事：第一，搭建了独立的 Config Server；第二，把它从单实例扩成了双实例；第三，把 `gateway` 和主要业务服务都接入配置中心；第四，引入 RabbitMQ 和 Spring Cloud Bus，让这些服务都加入刷新广播链路；第五，在 `order-service` 里补了一个 `@RefreshScope` 演示接口，用来现场证明热刷新效果。

## 2. 你为什么没有按课件使用远程 Git 配置仓库？

推荐回答：

> 我这里默认保留的是 native 模式，因为当前项目主要在本地和 Docker Compose 环境里联调，native 模式不依赖外网、Git 账号和 webhook，演示更稳定，也更容易复现。但我现在已经把 Git 模式的配置入口也补上了，只要切换 `CONFIG_SERVER_MODE=git` 并提供 `CONFIG_GIT_URI`，就可以切到更接近课件标准的做法。

## 3. 你现在这套方案还算不算配置中心？

推荐回答：

> 算。因为配置并不是写死在每个服务内部，而是由独立的 Config Server 统一对外提供。默认配置源是外部挂载目录，所以它是更稳的 native 替代实现；如果要贴近课件标准，也可以切到 Git 模式。

## 4. 为什么 Config Server 没有做成集群？

推荐回答：

> 这个点现在已经补上了。当前 compose 会启动两个 Config Server 实例，客户端也不再直连固定地址，而是通过 Eureka discovery-first 发现 `config-server` 服务。

## 5. 你为什么在 `bootstrap.yml` 里直接写 `config-server:8888`，而不是走 Eureka 发现？

推荐回答：

> 之前是为了简化启动链路才这么做，但现在为了更接近课件标准，我已经把它改成 discovery-first 了。这样客户端看到的是统一的 `config-server` 服务名，而不是某个固定节点地址，更符合集群化配置中心的设计。

## 6. 现在有哪些服务真正接入了 Config Client？

推荐回答：

> 当前接入的是 `gateway`、`order-service`、`user-service`、`merchant-service`、`product-service`、`cart-service`、`address-service`、`points-service`、`wallet-service`。这些服务仍保留本地配置作为兜底，但主配置源已经迁移到了 Config Server。

## 7. 为什么不是所有服务都接入配置中心？

推荐回答：

> 我最开始确实采用了分批迁移策略，但现在已经把主要服务都迁过去了。保留本地配置的原因主要不是“还没迁”，而是为了在 `fail-fast: false` 的策略下保留启动兜底能力。

## 8. Bus 真的接进来了吗，还是只写在文档里？

推荐回答：

> Bus 是真的接进来了。代码里 `config-server`、`gateway` 和主要业务服务都有 `spring-cloud-starter-bus-amqp` 依赖，`docker-compose.yml` 也确实部署了 RabbitMQ，同时 Config Server 暴露了 `bus-refresh` 端点，所以这不是纸面设计，而是已经进入工程实现。

## 9. 你为什么选 RabbitMQ？

推荐回答：

> Spring Cloud Bus 常见就是 RabbitMQ 或 Kafka。RabbitMQ 在课程项目里部署成本更低、管理界面直观、配置也更简单，所以更适合作为 Bus 的消息中间件。

## 10. 课件里写的是 `spring-cloud-bus` 和 Rabbit Binder，你为什么用了 `spring-cloud-starter-bus-amqp`？

推荐回答：

> 因为当前项目是 Spring Boot 3.3 和 Spring Cloud 2023 的技术栈，这个版本里更常用的是 starter 形式的 `spring-cloud-starter-bus-amqp`。本质上它还是基于 AMQP 和 RabbitMQ 的 Bus，只是依赖写法更符合新版栈。

## 11. `/actuator/bus-refresh` 和 `/actuator/refresh` 有什么区别？

推荐回答：

> `/actuator/refresh` 更偏向单个应用上下文刷新；`/actuator/bus-refresh` 是通过消息总线把刷新事件广播出去，适合多实例、多服务场景。当前我更关注的是 Bus 广播，所以重点开放的是 `bus-refresh`。

## 12. 你现在修改配置后，所有服务都会自动刷新吗？

推荐回答：

> 现在可以更积极一点回答。当前主要服务都已经接入 Bus，至少在 `order-service` 上我已经补了 `@RefreshScope` 演示点，所以我可以证明“Bus 刷新链路和业务热刷新效果都成立”。但我不会夸张到说“所有配置项都一定支持无感热更新”，因为这仍取决于具体 Bean 的加载方式。

## 13. 你为什么要强调 `@RefreshScope`？

推荐回答：

> 因为 Bus 只负责把“刷新事件”广播出去，但具体某个 Bean 能不能在运行时重新读取配置，还要看这个 Bean 的刷新机制。`@RefreshScope` 就是最常见的实现方式。如果没有这一层，很多通过 `@Value` 注入到普通单例 Bean 的值，不一定能形成一个明显的热更新效果。

## 14. 那你当前实现是不是没有真正完成动态刷新？

推荐回答：

> 现在不能再说“只有基础设施接入”。更准确地说，是“动态刷新已经完成了基础设施接入，并且我补了一个可验证的业务演示点”。还没有完全做满的是 Config Server 集群和 Git 配置仓库，不是热刷新本身完全缺失。

## 15. 你为什么保留 `fail-fast: false`？

推荐回答：

> 这是为了提高系统启动时的容错性。配置中心如果暂时不可用，服务仍然可以用本地配置先启动，避免单点故障直接阻断整个系统联调。这是一个偏工程实践的取舍。

## 16. 这会不会让配置不一致？

推荐回答：

> 会增加“配置中心不可用时退回本地配置”的一致性风险，所以它更适合开发和演示环境。在更严格的生产环境里，可以把 `fail-fast` 打开，或者进一步配合统一配置仓库和发布流程来控制一致性。

## 17. 你为什么说 native + volume mount 也算外部化配置？

推荐回答：

> 因为配置文件不在服务代码内部，而是在独立目录中，由 Config Server 统一读取和暴露。服务本身只负责通过 Config Client 获取配置。从“配置与代码分离”的角度看，这已经属于外部化配置，只是外部源不是 Git，而是宿主机目录。

## 18. gateway 为什么接了 Config Client，却没接 Bus？

推荐回答：

> 这个边界现在已经补掉了。gateway 已经接入了 Bus，所以它也属于当前广播刷新链路的一部分。

## 19. 如果让你继续完善，你下一步怎么做？

推荐回答：

> 我会优先补两件事。第一，继续完善 Git 配置仓库，比如接入远程仓库、分支和 webhook；第二，在更多服务中补业务级 `@RefreshScope` 演示点，把热刷新效果展示得更完整。

## 20. 如果老师问“这算不算完成作业”，你怎么回答最稳？

推荐回答：

> 如果按课件原版标准来衡量，这部分现在已经完成了一个更接近标准版的增强实现：配置中心、双实例部署、Eureka 发现、消息总线广播刷新、主要服务接入，以及热刷新演示点都已经进了代码和部署链路。还没有完全做成课件原版的地方主要是默认仍保留 native 模式，以及 Git 配置仓库还需要你在实际环境中接入具体仓库地址。

## 21. 如果老师问“你这套方案最大的优点是什么？”

推荐回答：

> 最大优点是稳定和可复现。它适合当前课程项目的 Docker Compose 本地部署环境，不依赖外部 Git 服务，配置修改和部署排查都比较直接，能比较稳地把 Config + Bus 这条主链路跑起来。

## 22. 如果老师问“你这套方案最大的缺点是什么？”

推荐回答：

> 最大缺点是仍然偏教学和演示环境。虽然我已经补了双实例和 discovery-first，但默认模式还是 native；如果要完全贴近课件标准，还需要把 Git 仓库地址、分支策略和自动同步流程真正接起来。