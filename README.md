# ELM 微服务实践项目（TJU SE）

本仓库当前以 `elm-v2.0` 作为统一入口（外部 API 聚合层），内部通过 RestTemplate 调用 8 个已拆分微服务：`points-service`、`account-service`、`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service`。

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

### 3.2 启动后端服务

```bash
docker compose up -d --build
```

启动后访问：

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

- 根目录 `docker-compose.yml` 默认启动后端、数据库和 Spring Cloud 治理组件
- 前端开发联调优先通过 `http://localhost:8090` 访问网关，再由网关转发到 `elm-v2.0` 或各个下游服务
- `frontend` 已放入可选 profile，便于先完成后端联调

### 3.3 联调运行顺序（推荐）

建议按下面顺序做新版方案联调：

1. 启动后端编排：`docker compose up -d --build`
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
4. 如需前端联调：
  - `cd elm-frontend && pnpm install && pnpm dev --host 0.0.0.0`
  - 浏览器访问 Vite 地址，前端会通过代理访问 `http://localhost:8090`

说明：

- 前端联调优先走网关 `8090`，这样更接近容器部署与课程验收入口
- 若只排查聚合层，可临时直接访问 `http://localhost:8080/elm`
- `run_four_service_smoke.py` 主要验证后端主链路，不负责前端页面回归

### 3.4 如需同时启动前端

```bash
docker compose --profile frontend up -d --build
```

启动后可访问：

- 前端：`http://localhost`

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

## 4. 部署说明

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

## 6. 无 Docker 的本地后端运行

当当前环境无法使用 Docker 时，可以直接运行后端服务。

### 6.1 方案说明

- 使用工作区内的 JDK 21 和 Maven 3.9.x
- 使用 Spring Boot `local` profile
- 使用 H2 文件数据库替代 MySQL，数据库文件保存在 `.localdb/`
- 启动脚本位于 `scripts/run-local-backend.sh`

### 6.2 启动步骤

先准备工作区运行时：

```bash
mkdir -p .tools .m2 .localdb .logs .run
```

随后安装 JDK 和 Maven 到 `.tools/`，建议按“基础设施 -> 业务服务”顺序执行：

```bash
bash scripts/run-local-cloud.sh
bash scripts/run-local-backend.sh
```

其中：

- `run-local-cloud.sh` 启动 `config-server`、`discovery-server`、`gateway-service`
- `run-local-backend.sh` 启动 `points/account/business/food/cart/order/address/user/elm-v2`

只需要业务服务时，也可以单独执行：

```bash
bash scripts/run-local-backend.sh
```

停止服务：

```bash
bash scripts/stop-local-backend.sh
bash scripts/stop-local-cloud.sh
```

## 7. 本地前端运行

前端当前通过 Vite 代理访问后端，开发环境默认代理到本地 Spring Cloud Gateway `http://localhost:8090`。

```bash
cd elm-frontend
corepack enable
corepack prepare pnpm@10.32.1 --activate
pnpm install
pnpm dev --host 0.0.0.0
```

联调前建议先确认：

- `gateway-service` 已监听 `8090`
- `elm-v2` 已监听 `8080`
- `user-service`、`address-service`、`cart-service`、`order-service` 至少各有一个实例已启动

默认访问地址：

- `http://localhost:5173`

如果 `5173` 已被占用，Vite 会自动切换到下一个可用端口，例如 `5174`。

## 8. Spring Cloud 治理组件起步

当前仓库已新增 3 个渐进接入模块：

- `elm-microservice/config-server/`：配置中心，端口 `8888`
- `elm-microservice/discovery-server/`：Eureka 注册中心，端口 `8761`
- `elm-microservice/gateway-service/`：网关，端口 `8090`

本地配置仓库目录：

- `spring-cloud-config-repo/`

启动治理组件：

```bash
bash scripts/run-local-cloud.sh
```

停止治理组件：

```bash
bash scripts/stop-local-cloud.sh
```

当前网关是渐进接入方式：

- `/api/**` 转发到现有 `elm-v2.0`
- `/elm/**` 保持直通现有聚合服务
- `/services/points/**`、`/services/account/**`、`/services/business/**`、`/services/food/**`、`/services/cart/**`、`/services/order/**`、`/services/address/**`、`/services/user/**` 可直接转发到对应微服务

这意味着你可以先保留现有 `elm-v2.0` 聚合层，后续再逐步把前端入口和服务调用切到真正的 Spring Cloud 治理链路上。

## 9. Spring Cloud 客户端接入

当前已给下列业务服务补充 `config client + eureka client` 的接入能力，并保留 `local` 直跑模式：

- `elm-v2.0`
- `business-service`
- `food-service`
- `cart-service`
- `order-service`
- `address-service`
- `account-service`
- `points-service`

其中 `elm-v2.0` 已改为使用 Spring 容器提供的 `RestTemplate`，可在 `cloud` profile 下通过服务名访问下游服务，并由 Spring Cloud LoadBalancer 对集群实例进行负载均衡。

## 10. 本地高可用集群运行

启动顺序建议：

```bash
bash scripts/run-local-cloud.sh
bash scripts/run-local-backend-cloud.sh
```

停止后端：

```bash
bash scripts/stop-local-backend.sh
```

当前本地高可用实例布局：

- `business-service-a`: `8083`
- `business-service-b`: `8183`
- `food-service-a`: `8087`
- `food-service-b`: `8187`
- `cart-service-a`: `8089`
- `cart-service-b`: `8189`
- `order-service-a`: `8084`
- `order-service-b`: `8184`
- `address-service`: `8085`
- `user-service`: `8086`
- `account-service`: `8082`
- `points-service`: `8081`
- `elm-v2.0`: `8080`

按课程要求映射：

- 商家：由 `business-service` 双实例集群承载
- 食品：由 `food-service` 双实例集群承载
- 购物车：由 `cart-service` 双实例集群承载
- 订单：由 `order-service` 双实例集群承载
- 送货地址：由单实例 `address-service` 承载
- 用户：由单实例 `user-service` 承载，`elm-v2.0` 保留兼容入口

本地无 Docker 的云模式说明：

- `business-service` 和 `food-service` 当前共用 `.localdb/elm_catalog`，脚本已改为顺序启动以规避 H2 启动期的文件锁竞争
- 生产或容器化场景下建议继续使用 MySQL，以获得更稳定的多实例共享存储行为
