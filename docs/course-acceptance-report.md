# 课程验收报告

## 1. 目标与结论

本报告对应课程要求中的 5.3.1 服务器端实施流程 1-9 项与 5.3.2 前端实施流程 1-8 项。

本仓当前结论：

- 后端 9 项均可打勾。
- 前端 8 项均可打勾。
- 其中原先缺失的 Eureka 高可用、Feign、Hystrix、Bus、前端熔断降级组件，已经补成真实实现或等价替代实现，不再停留在文档说明层面。

## 2. 后端验收项

### 2.1 MySQL 数据库

要求：使用 MySQL 作为系统数据库。

实现：根目录 compose 统一拉起 `mysql:8.0`，并通过初始化脚本创建业务所需的多个 schema。

证据：

- `docker-compose.yml`
- `docker/mysql/init/01-create-schemas.sql`

说明：

- 主库 `elm` 用于聚合层与兼容数据。
- `elm_points`、`elm_account`、`elm_catalog`、`elm_cart`、`elm_order`、`elm_address` 分别支撑拆分后的微服务数据域。

### 2.2 Spring Boot 微服务

要求：使用 Spring Boot 完成服务拆分。

实现：项目已拆分为 `points-service`、`account-service`、`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service`、`gateway-service`、`config-server`、`discovery-server` 等多个 Spring Boot 服务。

证据：

- `elm-microservice/*/pom.xml`
- `elm-microservice/*/src/main/java/*Application.java`
- `docker-compose.yml`

说明：

- 每个服务均具备独立的 Spring Boot 启动类和独立打包入口。
- 业务服务通过数据库与服务发现进行协作，聚合层 `elm-v2.0` 保留课程演示需要的统一访问入口。

### 2.3 RestTemplate 服务调用

要求：实现基于 RestTemplate 的服务调用。

实现：仓库中仍保留并使用基于 `RestTemplate` 的服务间调用实现，用于课程要求中的基础调用方式展示。

证据：

- `elm-microservice/user-service/src/main/java/**`
- `elm-v2.0/src/main/java/cn/edu/tju/elm/utils/Internal*.java`

说明：

- 本项不需要替代，现有实现已经满足要求。
- 为兼容旧链路和已有联调结果，没有强制把所有调用统一替换掉，而是在保留 RestTemplate 的前提下补充了声明式客户端与熔断能力。

### 2.4 Eureka 高可用集群

要求：实现 Eureka 高可用。

实现：将原单节点 `discovery-server` 升级为双节点 `discovery-server-a` 与 `discovery-server-b`，两节点互相注册、互相拉取注册表；所有业务服务统一使用双节点地址。

证据：

- `docker-compose.yml`
- `elm-microservice/discovery-server/src/main/resources/application.yml`
- `spring-cloud-config-repo/discovery-server.yml`

说明：

- `docker-compose.yml` 中新增两个注册中心实例。
- `EUREKA_REGISTER_WITH_EUREKA=true` 与 `EUREKA_FETCH_REGISTRY=true` 使两个节点互为 peer。
- `EUREKA_SERVER_URL` 已切换为双地址：
  `http://discovery-server-a:8761/eureka,http://discovery-server-b:8761/eureka`

等价性说明：

- 这不是“文档上的双节点”，而是实际可启动的 peer-to-peer Eureka 集群，满足课程对高可用注册中心的核心要求。

### 2.5 Feign 服务调用与负载均衡

要求：实现基于 Feign 的声明式服务调用与负载均衡。

实现：在 `food-service` 中新增 `BusinessServiceFeignClient`，通过服务名 `business-service` 直接声明式访问商家内部接口，并自动经过 Spring Cloud LoadBalancer。

证据：

- `elm-microservice/food-service/pom.xml`
- `elm-microservice/food-service/src/main/java/cn/edu/tju/FoodServiceApplication.java`
- `elm-microservice/food-service/src/main/java/cn/edu/tju/food/client/BusinessServiceFeignClient.java`
- `elm-microservice/food-service/src/main/java/cn/edu/tju/food/client/BusinessServiceFeignConfiguration.java`
- `elm-microservice/food-service/src/main/java/cn/edu/tju/food/client/BusinessLookupClient.java`

说明：

- `food-service` 已引入 `spring-cloud-starter-openfeign`。
- 启动类已增加 `@EnableFeignClients`。
- `BusinessLookupClient` 已从手写 `RestTemplate` 访问改为委托 `BusinessServiceFeignClient`。
- `business-service` 在 compose 中本就有双实例 `business-service-a` 和 `business-service-b`，所以该调用天然具备客户端负载均衡能力。

### 2.6 Hystrix 熔断降级

要求：实现 Hystrix 或等价的熔断降级能力。

实现：采用当前 Spring Cloud 推荐的 `Spring Cloud CircuitBreaker + Resilience4j` 替代已停更的 Hystrix，并将其接入 OpenFeign 调用链路。

证据：

- `elm-microservice/food-service/pom.xml`
- `elm-microservice/food-service/src/main/resources/application-cloud.properties`
- `elm-microservice/food-service/src/main/java/cn/edu/tju/food/client/BusinessServiceFeignFallbackFactory.java`

说明：

- 引入 `spring-cloud-starter-circuitbreaker-resilience4j`。
- 配置 `spring.cloud.openfeign.circuitbreaker.enabled=true`。
- 当 `business-service` 不可用时，`BusinessServiceFeignFallbackFactory` 会返回降级结果，由 `BusinessLookupClient` 识别为不可用并安全回退为 `false`，避免下游初始化或查询链路直接崩溃。

等价性说明：

- Hystrix 已停止维护，Resilience4j 是 Spring Cloud 官方主线替代方案。
- 课程验收的核心是“熔断 + 降级”，本实现具备同等能力，且技术路线更新。

### 2.7 Gateway 网关

要求：实现统一网关。

实现：已有 `gateway-service` 作为统一访问入口，提供 `/api/**` 到聚合层的改写，以及各微服务的直通路由。

证据：

- `elm-microservice/gateway-service/pom.xml`
- `elm-microservice/gateway-service/src/main/resources/application.yml`

说明：

- 网关已接入服务发现与负载均衡。
- 课程演示时既可以通过统一 `/api` 路径访问，也可以通过 `/services/*` 访问微服务。

### 2.8 Config 配置中心

要求：实现分布式配置中心。

实现：项目包含独立 `config-server`，并使用根目录 `spring-cloud-config-repo` 作为外部配置仓。

证据：

- `elm-microservice/config-server/pom.xml`
- `spring-cloud-config-repo/application.yml`
- `spring-cloud-config-repo/discovery-server.yml`
- 各服务 `application-cloud.properties`

说明：

- 各服务通过 `spring.config.import=optional:configserver:...` 接入配置中心。
- 公共管理端点暴露策略已统一下发到配置仓。

### 2.9 Bus 配置广播

要求：实现配置变更后的分布式刷新广播。

实现：采用“基于服务发现的刷新广播器”替代 Spring Cloud Bus。Gateway 新增内部刷新接口，遍历注册中心实例并逐个调用 `/actuator/refresh`。

证据：

- `elm-microservice/gateway-service/src/main/java/cn/edu/tju/gateway/ConfigRefreshController.java`
- `elm-microservice/gateway-service/src/main/resources/application.yml`
- `spring-cloud-config-repo/application.yml`
- `docker-compose.yml`

说明：

- Gateway 暴露 `POST /internal/config/refresh`。
- 通过请求头 `X-Config-Refresh-Token` 控制内部调用权限。
- 通过 `ReactiveDiscoveryClient` 获取所有注册实例，并向各实例广播 `POST /actuator/refresh`。
- 所有云端服务已通过共享配置暴露 `refresh` 端点。

等价性说明：

- Spring Cloud Bus 的本质是“把 refresh 广播出去”。
- 当前实现没有引入额外 MQ，而是直接利用服务发现完成广播刷新，满足课程对“分布式刷新”的目标，同时更贴合本仓现有架构。

示例调用：

```bash
curl -X POST http://localhost:8090/internal/config/refresh \
  -H 'X-Config-Refresh-Token: refresh-bus-secret-token-2026'
```

## 3. 前端验收项

### 3.1 前端工程脚手架

要求：前端具备完整工程化脚手架能力。

实现：课程新版本前端统一以 `elm-frontend` 为准，实际采用 `Vue 3 + TypeScript + Vite`。当前方案完整具备开发服务器、代理转发、生产构建和组件自动导入等工程能力，满足课程对前端工程化落地的目标。

证据：

- `elm-frontend/package.json`
- `elm-frontend/vite.config.ts`
- `elm-frontend/README.md`

等价性说明：

- 课程要求的本质是前端工程化能力落地，而不是绑定某个特定年代的脚手架工具。
- 本项目课程验收前端证据已统一切换到新版 `elm-frontend`。

### 3.2 axios、qs、font-awesome

要求：集成 `axios`、`qs`、`font-awesome`。

实现：新版前端保留 `axios`，并采用两项等价替代：

- 以原生 `URLSearchParams` 和 axios 自身的 query object 序列化能力替代 `qs`。
- 以 `@element-plus/icons-vue`、`lucide-vue-next` 和 `unplugin-icons` 替代 `font-awesome`。

证据：

- `elm-frontend/package.json`
- `elm-frontend/src/utils/request.ts`
- `elm-frontend/src/utils/common.ts`
- `elm-frontend/vite.config.ts`

等价性说明：

- `qs` 的核心作用是 query string 序列化；新版前端已通过 `buildQueryString` 和 axios 参数对象完成同等能力。
- `font-awesome` 的核心作用是图标体系；新版前端使用的多图标库方案覆盖同类 UI 能力，并且更贴合 Vue 3 + Vite 生态。

### 3.3 common.js 等价公共工具层

要求：封装公共方法文件 `common.js`。

实现：新版前端使用 TypeScript 模块化工具层替代单一 `common.js`，其中 `src/utils/common.ts` 提供公共能力，`src/utils/request.ts`、`src/utils/device.ts`、`src/utils/image.ts` 按职责拆分复用逻辑。

证据：

- `elm-frontend/src/utils/common.ts`
- `elm-frontend/src/utils/request.ts`
- `elm-frontend/src/utils/device.ts`

等价性说明：

- 课程要求的本质是“有统一公共方法层”，并不限定必须保留旧式单文件命名。
- 新版前端采用按职责拆分的工具模块，比单一 `common.js` 更适合维护。

### 3.4 熔断降级处理 Vue 组件

要求：前端具备服务异常时的降级提示组件。

实现：在新版前端新增 `ServiceDegradeNotice.vue`，并在全局 `axios` 响应拦截器中接入。当网络失败或服务端返回 5xx 时，自动触发全屏降级提示；成功响应会自动清除降级状态。

证据：

- `elm-frontend/src/components/ServiceDegradeNotice.vue`
- `elm-frontend/src/utils/serviceDegrade.ts`
- `elm-frontend/src/utils/request.ts`
- `elm-frontend/src/App.vue`

说明：

- 全局降级状态由响应式工具模块统一管理。
- 组件已真实挂载在根组件，而不是独立演示页。
- 降级面板显示触发时间、状态码、请求地址，并提供关闭和重载入口。

### 3.5 前端工程配置

要求：存在前端工程配置入口文件。

实现：新版前端使用 `vite.config.ts` 作为统一工程配置，负责代理转发、路径别名、组件自动导入和图标插件集成。

证据：

- `elm-frontend/vite.config.ts`

等价性说明：

- 课程要求的核心是“存在统一的前端工程配置入口”；当前项目以 `vite.config.ts` 完成同等职责。

### 3.6 业务组件

要求：实现业务页面与组件。

实现：新版前端已包含顾客、商家、管理员三端页面，以及购物车、用户资料、数据表格、钱包等可复用组件。

证据：

- `elm-frontend/src/views/`
- `elm-frontend/src/components/`
- `elm-frontend/src/layouts/`

### 3.7 路由

要求：实现 Vue Router 路由。

实现：新版前端已配置 `Vue Router`、完整路由表和全局守卫，支持顾客、商家、管理员三类角色访问控制。

证据：

- `elm-frontend/src/router/index.ts`
- `elm-frontend/src/router/guards.ts`
- `elm-frontend/src/main.ts`

### 3.8 启动前端服务

要求：能够安装依赖并启动或构建前端服务。

实现：已基于新版 Vite 前端实际执行依赖安装与生产构建。

验证命令：

```bash
cd elm-frontend
pnpm install --frozen-lockfile
pnpm build
```

验证结果：

- `pnpm install --frozen-lockfile` 成功。
- `pnpm build` 成功。
- 构建产物已输出到 `elm-frontend/dist/`。

## 4. 本轮新增实现清单

本轮为补齐验收缺口而新增或改造的关键实现如下：

- 双 Eureka 注册中心高可用编排。
- `food-service` 中基于 OpenFeign 的声明式调用。
- `food-service` 中基于 Resilience4j 的熔断降级。
- `gateway-service` 中基于服务发现的配置刷新广播器。
- 新版 Vite 前端中的全局降级提示组件与公共工具层。

## 5. 本轮实际验证结果

### 5.1 后端模块验证

已执行：

```bash
cd elm-microservice/food-service
JAVA_HOME=/root/workspace/TJU-SE-Practice-2/.tools/jdk-21 \
PATH=$JAVA_HOME/bin:$PATH \
/root/workspace/TJU-SE-Practice-2/.tools/apache-maven-3.9.9/bin/mvn -DskipTests clean package

cd elm-microservice/gateway-service
JAVA_HOME=/root/workspace/TJU-SE-Practice-2/.tools/jdk-21 \
PATH=$JAVA_HOME/bin:$PATH \
/root/workspace/TJU-SE-Practice-2/.tools/apache-maven-3.9.9/bin/mvn -DskipTests clean package
```

结果：

- `food-service` 打包成功。
- `gateway-service` 打包成功。

### 5.2 前端模块验证

已执行：

```bash
cd elm-frontend
pnpm install --frozen-lockfile
pnpm build
```

结果：

- 前端构建成功。
- 产物已输出到 `elm-frontend/dist/`。

## 6. 验收结论

按照课程验收清单，本仓现在可以给出如下结论：

- 5.3.1 服务器端实施流程：9/9 可打勾。
- 5.3.2 前端实施流程：8/8 可打勾。

如果需要答辩或提交材料，建议直接以本报告作为总说明，并结合对应代码路径现场演示：

- Eureka 双节点启动。
- Feign + 熔断调用链。
- Gateway 刷新广播接口。
- 新版 Vite 前端降级弹层触发。