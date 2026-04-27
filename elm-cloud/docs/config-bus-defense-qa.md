# Config + Bus 答辩问答清单

## 1. 你到底做了什么？

推荐回答：

> 我把配置中心和动态刷新这条链路完整落地了。现在项目里有两个 Config Server 实例，都会注册到 Eureka；`gateway` 和主要业务服务都通过 discovery-first 发现配置中心；RabbitMQ 和 Spring Cloud Bus 已经接通；配置统一放在 `elm-cloud/config/` 目录下；另外我给各微服务 Controller 统一加了 `@RefreshScope`，并提供了 `order-service` 的 `/elm/api/orders/runtime-config` 演示接口，用来现场证明热刷新生效。

## 2. 你现在这套方案还算不算配置中心？

推荐回答：

> 算。因为配置已经不再分散写死在每个服务内部，而是由独立的 Config Server 统一读取和对外提供。服务本身只负责通过 Config Client 获取配置，所以这已经是标准的集中配置管理思路。

## 3. Config Server 为什么要做成双实例？

推荐回答：

> 因为配置中心本身也是基础设施，如果只有一个实例，就会成为新的单点。现在 compose 里是 `config-server-1` 和 `config-server-2` 两个实例，都会注册成统一的 `config-server` 服务名，客户端通过 Eureka 发现它们。

## 4. 你为什么不用固定地址，而是走 Eureka 发现？

推荐回答：

> discovery-first 的好处是客户端不感知具体节点地址，只认 `config-server` 这个服务名。这样配置中心扩容、替换节点或者做高可用时，客户端配置不用跟着改，更符合集群化设计。

## 5. 现在有哪些服务接入了 Config Client？

推荐回答：

> 当前接入的是 `gateway`、`order-service`、`user-service`、`merchant-service`、`product-service`、`cart-service`、`address-service`、`points-service`、`wallet-service`。这些服务都通过 `bootstrap.yml` 在启动早期从 Config Server 拉取配置。

## 6. 现在有哪些服务接入了 Bus？

推荐回答：

> `config-server`、`gateway` 和主要业务服务都接入了 Bus。代码里能看到 `spring-cloud-starter-bus-amqp` 依赖，部署里也能看到 RabbitMQ，所以这不是纸面设计，而是已经跑起来的总线广播链路。

## 7. 你为什么选 RabbitMQ？

推荐回答：

> RabbitMQ 部署成本低，管理界面直观，跟 Spring Cloud Bus 的集成也比较直接，适合课程项目和本地演示环境。

## 8. `/actuator/busrefresh` 和 `/actuator/refresh` 有什么区别？

推荐回答：

> `/actuator/refresh` 更偏向单个应用上下文刷新，`/actuator/busrefresh` 会把刷新事件发到消息总线，再广播给整个集群。当前项目重点展示的是多服务、多实例场景，所以我主要演示 `busrefresh`。

## 9. 你现在能证明动态刷新真的生效了吗？

推荐回答：

> 能。现在我可以修改 `elm-cloud/config/order-service.yml` 里的演示字段，然后调用 `POST /actuator/busrefresh`，最后再访问 `/elm/api/orders/runtime-config`，就能看到返回值实时变化。

## 10. 你为什么要加 `@RefreshScope`？

推荐回答：

> 因为 Bus 负责广播“刷新事件”，但具体 Bean 能不能在运行时重新取值，还要看 Bean 自身是不是可刷新。`@RefreshScope` 就是这个刷新边界。没有它，很多单例 Bean 在启动后会把值固定住，看不到热更新效果。

## 11. 只加了 `@RefreshScope`，是不是就够了？

推荐回答：

> 不够。`@RefreshScope` 只解决“Bean 能刷新”，不解决“配置从哪里统一管理”。真正需要热刷新的配置项，还是要放到 Config Server 管理的中心化配置文件里。当前项目里这部分已经集中在 `elm-cloud/config/` 目录下了。

## 12. 你为什么要在所有 Controller 上都加 `@RefreshScope`？

推荐回答：

> 一方面这是为了和课程任务里“Controller 层支持动态刷新”的写法保持一致；另一方面这样做以后，如果某个接口直接依赖中心化配置，运行时刷新行为会更明确，答辩时也更容易解释。

## 13. 现在修改配置后，所有配置项都会自动刷新吗？

推荐回答：

> 我会更严谨地回答：当前这条刷新链路已经打通，并且所有 Controller 都具备刷新作用域能力。但一个具体配置项是否会表现出热更新，还要看它是不是通过可刷新的 Bean 读取，以及它对应的业务路径有没有被实际访问到。

## 14. 你为什么保留 `fail-fast: false`？

推荐回答：

> 这是为了提高启动阶段的容错性。配置中心如果短时间不可用，服务还能先用本地兜底配置启动，不至于整个联调环境直接起不来。这更符合课程项目和本地演示的稳定性需求。

## 15. 这会不会导致配置不一致？

推荐回答：

> 会带来“配置中心不可用时退回本地兜底配置”的一致性风险，所以它更适合开发和演示环境。当前我保留这个策略，是为了提高启动成功率和排障效率。

## 16. 你这套方案最大的优点是什么？

推荐回答：

> 最大优点是完整且稳定。现在已经具备双 Config Server、Eureka 发现、集中配置、Bus 广播、全 Controller `@RefreshScope` 和可观测演示接口，既能稳定跑起来，也能现场证明结果。

## 17. 你这套方案最大的限制是什么？

推荐回答：

> 最大限制是它仍然面向课程项目和本地 Docker Compose 环境做了工程化取舍，比如保留本地兜底配置和 `fail-fast: false`。但就“配置中心 + 广播刷新 + 可见演示”这条主链路而言，当前实现已经完整。

## 18. 如果老师问“这算不算完成作业”，你怎么回答最稳？

推荐回答：

> 可以说这一部分已经完成，并且代码、部署和演示链路是一致的。配置中心、双实例部署、Eureka 发现、RabbitMQ、Bus 刷新端点、主要服务接入、Controller 级 `@RefreshScope` 和运行时演示接口都已经落地。

## 19. 如果老师问“你下一步还能做什么”，你怎么回答？

推荐回答：

> 下一步我会做两类增强。第一类是把更多业务配置做成明确可观测的演示点，不只局限在 order-service；第二类是继续把答辩和验收脚本完善成一键式，让演示更稳定、复现更容易。
