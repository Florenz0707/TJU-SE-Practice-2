# ELM 微服务实践项目（TJU SE）

本仓库当前以 `elm-v2.0` 作为统一入口（外部 API 聚合层），内部通过 RestTemplate 调用 8 个已拆分微服务：`points-service`、`account-service`、`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service`。

## 文档入口

- 微服务完整实现说明：`docs/microservice-implementation-guide.md`
- 微服务验收答辩提纲：`docs/microservice-acceptance-defense.md`
- 微服务验收收口清单：`docs/microservice-acceptance-todo.md`
- 配置刷新失败恢复说明：`docs/config-refresh-recovery.md`
- 后端测试基线：`docs/backend-test-baseline.md`

## 模块文档索引

### 前端与入口

- `elm-frontend/README.md`：前端运行方式、代理口径和测试基线
- `elm-v2.0/README.md`：聚合层说明与本地运行方式

### 云治理层

- `elm-microservice/gateway-service/README.md`：网关路由、配置刷新与服务发现口径
- `elm-microservice/config-server/README.md`：配置中心说明
- `elm-microservice/discovery-server/README.md`：Eureka 注册发现说明

### 业务微服务

- `elm-microservice/account-service/README.md`：钱包、交易、券域
- `elm-microservice/points-service/README.md`：积分域
- `elm-microservice/business-service/README.md`：商家域
- `elm-microservice/food-service/README.md`：菜品与库存域
- `elm-microservice/cart-service/README.md`：购物车域
- `elm-microservice/order-service/README.md`：订单与评价域
- `elm-microservice/address-service/README.md`：地址域
- `elm-microservice/user-service/README.md`：用户与认证域

### 历史与中间态模块

- `elm-microservice/catalog-service/README.md`：目录域合并服务的历史中间态说明

## 1. 微服务业务调用流程

### 1.0 课程目标拆分与当前渐进实现

课程目标拆分：

- `business-service`：商家，集群部署
- `food-service`：食品，集群部署
- `cart-service`：购物车，集群部署
- `order-service`：订单，集群部署
- `address-service`：送货地址，单实例
- `user-service`：用户，单实例

结合当前仓库实现，现阶段采用渐进式拆分：

- `business-service` 已独立承载商家域，双实例部署
- `food-service` 已独立承载食品与库存域，双实例部署，并通过内部 HTTP 调用 `business-service` 校验商家归属
- `cart-service` 已独立承载购物车域，双实例部署
- `order-service` 继续承载订单与评价域，双实例部署
- `address-service` 已独立抽离为单实例服务，符合送货地址不集群化的课程要求
- `user-service` 已独立抽离为单实例服务，负责认证、JWT 签发、用户查询与注册
- `elm-v2.0` 当前保留外部 API 聚合、鉴权兼容层和跨域编排职责，对用户域改为远程调用 `user-service`
- `account-service`、`points-service` 属于课程要求之外的实践增强域服务，用于钱包、优惠券、积分等扩展能力

当前目标不是一次性推翻重做，而是在现有可运行链路上先补齐 Spring Cloud 治理能力、注册发现、配置中心和高可用实例，再继续做域拆分。

### 1.0.1 当前完成度对照

按课程要求核对，当前本地 `Spring Cloud + Eureka + Gateway` 运行模式已经满足下面这组目标：

- 六个目标微服务已独立存在：`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service`
- 商家、食品、购物车、订单四个服务已按双实例运行：
  - `business-service`: `8083` / `8183`
  - `food-service`: `8087` / `8187`
  - `cart-service`: `8089` / `8189`
  - `order-service`: `8084` / `8184`
- 送货地址、用户服务保持单实例：
  - `address-service`: `8085`
  - `user-service`: `8086`
- 食品微服务到商家微服务的内部调用关系已经落地：`food-service` 通过内部客户端调用 `business-service` 的 `/api/inner/business/{id}` 做商家存在性校验
- `order-service` 中旧的购物车实现已经删除，购物车域职责只保留在 `cart-service`

需要单独说明：

- 根目录 `docker-compose.yml` 已升级为 Spring Cloud 容器编排，包含 `config-server`、`discovery-server`、`gateway-service` 与 4 个双实例业务服务集群
- 当前“本地云模式”和“容器 compose 模式”在服务拆分目标上已经对齐：`business`、`food`、`cart`、`order` 为双实例，`address`、`user` 为单实例
- 运行态核验仍建议以 Eureka 注册结果为准；本地已验证双实例注册：`business-service-8083/8183`、`food-service-8087/8187`、`cart-service-8089/8189`、`order-service-8084/8184`
- 2026-03-31 已进一步收口 compose 启动链：`config-server`、`discovery-server`、`elm-v2`、`gateway-service` 增加了健康检查，关键依赖已从 `service_started` 收紧到 `service_healthy`
- 2026-03-31 已进一步收口 Gateway 直通路由：在 compose 场景下，`/services/*` 与 `/api/*` 相关上游已统一切到 `lb://service-id`，不再依赖固定容器地址，从而让网关层本身也体现服务发现与负载均衡
- 2026-03-31 已进一步收口内部鉴权策略：compose 验收环境不再为 `INTERNAL_SERVICE_TOKEN` 和 `CONFIG_REFRESH_TOKEN` 提供固定默认值，必须在 `.env` 中显式配置

### 1.1 下单链路（创建订单）

1. 前端调用 `elm-v2.0`：`POST /elm/api/orders`
2. `elm-v2.0` 校验用户、地址、购物车
3. `elm-v2.0 -> business-service`：查询商家快照
4. `elm-v2.0 -> food-service`：查询菜品、预占库存
5. `elm-v2.0 -> account-service`：扣钱包、核销券
6. `elm-v2.0 -> points-service`：冻结并扣减积分（如使用积分）
7. `elm-v2.0 -> order-service`：创建订单主记录和明细
8. `elm-v2.0 -> cart-service`：清理购物车
9. 返回下单结果给前端

### 1.2 取消订单链路

1. 前端调用 `elm-v2.0`：`POST /elm/api/orders/{id}/cancel`
2. `elm-v2.0 -> account-service`：退款钱包、回滚券
3. `elm-v2.0 -> points-service`：积分返还/回滚
4. `elm-v2.0 -> food-service`：库存回补
5. `elm-v2.0 -> order-service`：订单状态改为取消
6. 返回取消结果

### 1.3 订单完成与评价链路

1. 前端更新订单状态或提交评价到 `elm-v2.0`
2. `elm-v2.0 -> order-service`：更新订单/评价数据
3. `elm-v2.0` 写 Outbox 事件（订单完成积分、评价积分）
4. Outbox 调度后 `elm-v2.0 -> points-service` 发放积分

### 1.4 服务拓扑（逻辑）

```mermaid
flowchart LR
  FE[elm-frontend] --> GW[elm-v2.0]
  GW --> CRT[cart-service]
  GW --> ORD[order-service]
  GW --> ADR[address-service]
  GW --> BUS[business-service]
  GW --> FOD[food-service]
  GW --> ACC[account-service]
  GW --> PTS[points-service]

  GW --> DB0[(elm)]
  CRT --> DB6[(elm_cart)]
  ORD --> DB1[(elm_order)]
  ADR --> DB5[(elm_address)]
  BUS --> DB2[(elm_catalog)]
  FOD --> DB2[(elm_catalog)]
  ACC --> DB3[(elm_account)]
  PTS --> DB4[(elm_points)]
```

## 2. 服务边界（业务职责）

- `elm-v2.0`：外部 API、鉴权上下文、跨域编排、兼容层、Outbox
- `cart-service`：购物车
- `order-service`：订单、订单明细、评价
- `address-service`：配送地址
- `business-service`：商家
- `food-service`：菜品、库存预占/回补
- `account-service`：钱包、交易、券核销与回滚
- `points-service`：积分账户、交易、规则

## 3. 后端优先的 Docker 部署

### 3.1 准备环境变量

复制示例配置：

```bash
cp .env.example .env
```

按需修改 `.env` 中的敏感信息（数据库密码、内部 token）。

注意：compose 验收口径下，`INTERNAL_SERVICE_TOKEN` 与 `CONFIG_REFRESH_TOKEN` 必须显式填写；仓库已不再为这两个内部凭据提供固定默认值回退。

执行 `docker compose` 前请先确认 Docker Engine 和 Compose 插件可用。

如果你当前是在 VS Code dev container 或普通容器里开发，`docker` 命令很可能不可用，或者没有挂载宿主机 Docker Socket。这种情况下需要回到宿主机终端执行下面的 compose 命令，而不是在开发容器内部执行。

### 3.2 启动整套容器服务

```bash
cp .env.example .env
docker compose up -d --build
```

首次执行前至少应修改：

- `INTERNAL_SERVICE_TOKEN`
- `CONFIG_REFRESH_TOKEN`

如果第一次全量启动时宿主机资源较紧张，仍然可以改用分阶段启动；不过当前 compose 已为核心治理链路补上健康检查与 `service_healthy` 依赖，冷启动稳定性已经明显优于此前版本：

```bash
docker compose up -d --build mysql mysql-init config-server
docker compose up -d --build discovery-server-a discovery-server-b
docker compose up -d --build points-service account-service business-service-a business-service-b food-service-a food-service-b cart-service-a cart-service-b order-service-a order-service-b address-service user-service elm-v2 gateway-service frontend
```

推荐在每一阶段之间检查：

- `http://localhost:8888/actuator/health`
- `http://localhost:8761`
- `http://localhost:8090/actuator/health`

启动后访问：

- 前端：`http://localhost`
- 配置中心：`http://localhost:8888`
- 注册中心：`http://localhost:8761`
- Spring Cloud Gateway：`http://localhost:8090`
- 聚合 API：`http://localhost:8080/elm`
- points-service：`http://localhost:8081/elm`
- account-service：`http://localhost:8082/elm`
- business-service：`http://localhost:8083/elm`、`http://localhost:8183/elm`
- food-service：`http://localhost:8087/elm`、`http://localhost:8187/elm`
- cart-service：`http://localhost:8089/elm`、`http://localhost:8189/elm`
- order-service：`http://localhost:8084/elm`、`http://localhost:8184/elm`
- address-service：`http://localhost:8085/elm`
- user-service：`http://localhost:8086/elm`

说明：

- 根目录 `docker-compose.yml` 默认启动前端、后端、数据库和 Spring Cloud 治理组件
- 课程验收或一键演示时优先使用 Docker Compose；日常开发和排障也支持仓库自带工具链的本地直跑模式
- `elm-v1.0` 是历史代码，不参与当前部署、联调和验收
- 前端容器优先通过 `http://localhost` 访问，由 Nginx 转发到 `gateway-service`

### 3.3 本地直跑（不使用 Docker Compose）

适用于当前容器内开发、单服务重启和链路排障。

1. 启动配置中心、注册中心、网关：

```bash
bash scripts/run-local-cloud.sh
```

2. 启动业务服务与聚合层：

```bash
bash scripts/run-local-backend-cloud.sh
```

3. 如需单独启动或重启聚合层：

```bash
cd elm-v2.0
export JAVA_HOME=/root/workspace/TJU-SE-Practice-2/.tools/jdk-21
export PATH="$JAVA_HOME/bin:/root/workspace/TJU-SE-Practice-2/.tools/apache-maven-3.9.9/bin:$PATH"
mvn -Dmaven.test.skip=true -Dspring-boot.run.profiles=local,cloud spring-boot:run
```

4. 停止本地直跑服务：

```bash
bash scripts/stop-local-backend.sh
bash scripts/stop-local-cloud.sh
```

5. 当前推荐验证入口：

- 网关：`http://localhost:8090`
- 聚合层 Swagger：`http://localhost:8080/swagger-ui/index.html`
- 前端开发环境：`elm-frontend`

### 3.4 联调运行顺序（推荐）

建议按下面顺序做新版方案联调：

1. 启动后端编排：`docker compose up -d --build`
  - 若在 dev container 中执行提示 `docker: command not found`，说明当前环境不具备 Docker 运行条件，需要切回宿主机终端执行
  - 若一次性全量启动后出现配置中心或注册中心未就绪导致的级联失败，按 3.2 的分阶段方式重试
2. 检查关键入口：
  - `http://localhost:8888`
  - `http://localhost:8761`
  - `http://localhost:8090/actuator/health`
  - `http://localhost:8080/swagger-ui/index.html`
3. 执行业务 smoke：
  - `cd elm-v2.0/scripts`
  - `cp integration.env.example .env`
  - `uv sync`
  - `uv run run_four_service_smoke.py --env-file .env --skip-start`
4. 前端联调：
  - 直接访问 `http://localhost`
  - 前端容器会通过 Nginx 代理访问 `http://gateway-service:8090`

说明：

- 前端联调优先走容器入口 `http://localhost`，这样更接近课程验收时的完整部署入口
- 若只排查聚合层，可临时直接访问 `http://localhost:8080/elm`
- `run_four_service_smoke.py` 主要验证后端主链路，不负责前端页面回归

### 3.5 停止与清理

```bash
docker compose down
```

如需清理数据库卷：

```bash
docker compose down -v
```

### 3.6 首次启动失败排查（MySQL 未初始化）

如果出现“表不存在/服务反复重启”，通常是旧 `mysql-data` 卷导致初始化脚本未重跑。处理方式：

```bash
docker compose down -v
docker compose up -d --build
```

说明：

- 当前编排新增了 `mysql-init` 一次性初始化服务，会显式创建 `elm*` schema
- 各服务 `DB_URL` 也开启了 `createDatabaseIfNotExist=true` 作为兜底
- 容器内服务默认通过 `config-server + discovery-server` 获取配置并注册到 Eureka，`gateway-service` 与 `elm-v2.0` 通过逻辑服务名访问下游集群

## 4. 最新验证结论（2026-03-31）

本地直跑模式下，已完成一轮只走标准聚合接口的真实回归：

- 注册、登录成功
- `GET /api/wallet/my`、`POST /api/wallet/my/topup` 成功
- 购物车、地址、下单、取消、完成成功
- 评价新增、查询、删除成功
- `GET /api/orders/user/my` 返回正确订单列表

本轮回归同时确认：

- `elm-v2.0` 的钱包与交易实现已收口到 `account-service`
- `/api/wallet` 与 `/api/orders` 已使用同一资金源
- 不再需要通过 `/services/account/api/wallet/*` 直通充值来绕过聚合层

后端自动化测试与覆盖空白的最新盘点见：`docs/backend-test-baseline.md`

常用验证命令：

- 前端测试：`pnpm --dir elm-frontend test:run`
- 前端测试 + 构建：`pnpm --dir . run test:frontend:all`
- 前端构建：`pnpm --dir elm-frontend build`
- 网关 API 冒烟：`pnpm --dir . run smoke:gateway-api`

## 5. 部署说明

- 所有服务统一由根目录 `docker-compose.yml` 编排
- 编排包含 `config-server`、`discovery-server`、`gateway-service`，用于配置中心、服务注册发现和统一入口
- MySQL 使用 `docker/mysql/init/01-create-schemas.sql` 初始化多 schema：
  - `elm`
  - `elm_order`
  - `elm_cart`
  - `elm_address`
  - `elm_catalog`
  - `elm_account`
  - `elm_points`
- 双实例高可用服务为：`business-service-a/b`、`food-service-a/b`、`cart-service-a/b`、`order-service-a/b`
- 单实例服务为：`address-service`、`user-service`、`account-service`、`points-service`、`elm-v2`
- 微服务容器间调用统一走 Docker 网络内的逻辑服务名与 Eureka 注册名，例如 `http://business-service/elm`、`http://order-service/elm`

## 5. 代码目录

- `elm-v2.0/`：聚合层（对前端开放）
- `elm-microservice/order-service/`
- `elm-microservice/address-service/`
- `elm-microservice/business-service/`
- `elm-microservice/food-service/`
- `elm-microservice/cart-service/`
- `elm-microservice/account-service/`
- `elm-microservice/points-service/`
- `docker-compose.yml`：统一部署入口

## 6. 部署约束

- 当前仓库的标准部署方式是根目录 `docker compose up -d --build`
- 当前环境禁止通过宿主机直接安装或运行 JDK、Maven、pnpm
- `scripts/run-local-*.sh` 仅保留为历史调试脚本，不属于当前容器化部署方案
- `elm-v1.0` 为过时版本，当前实施、联调与验收全部忽略
