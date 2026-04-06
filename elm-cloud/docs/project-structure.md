# elm-cloud 项目结构梳理

> 作用：把 `elm-cloud` 下的 **Docker Compose 微服务项目**按「目录/服务/职责/依赖/运行方式」做一份总览，方便你在课程阶段做联调、排错与后续扩展。

## 1. 一句话总览

`elm-cloud` 是一个 **Maven 多模块** 的 Spring Cloud 微服务项目：

- **Eureka** 做服务注册发现
- **Config Server** 做配置中心（当前以 native/classpath 示例）
- **Gateway** 做统一入口与路由
- 业务域拆分为用户、商家、商品、购物车、订单、地址、积分、钱包等多个微服务
- **MySQL 8** 作为底层数据库，由 `docker/mysql/init` 初始化
- `docker-compose.yml` 一键拉起：数据库 + 注册中心 + 配置中心 + 网关 + 各业务服务 + 前端

## 2. 目录结构（你最常用的入口）

- `docker-compose.yml`
  - 本地一键启动编排文件（包含 mysql / eureka / config / gateway / 各业务服务 / frontend）。

- `pom.xml`
  - Maven 父工程（multi-module），各服务模块作为子 module。

- `docker/mysql/init/`
  - MySQL 初始化脚本：建库/建表/初始化账号等。

- `docs/`
  - 每个业务微服务的职责与接口梳理文档（当前已经有 user/order/product/cart/merchant 等）。

- `*/Dockerfile`
  - 每个微服务的镜像构建入口（Spring Boot 模块通常以 Maven build 输出 jar/BOOT-INF 的方式运行）。

- `gateway/`、`eureka-server/`、`config-server/`
  - 基础设施服务（注册中心 / 配置中心 / API 网关）。

- `XXX-service/`
  - 业务微服务模块。

- `elm-frontend/`
  - 前端容器（Nginx + 静态资源），对外映射 80 端口。

## 3. Compose 服务清单与依赖关系

`elm-cloud/docker-compose.yml` 中的服务（按依赖顺序）：

### 3.1 基础设施

- `mysql` (image: mysql:8.0)
  - 端口映射：`3306:3306`
  - volume：`mysql-data:/var/lib/mysql` + `./docker/mysql/init:/docker-entrypoint-initdb.d:ro`

- `eureka-server`
  - 端口：`8761:8761`
  - depends_on：mysql

- `config-server`
  - 端口：`8888:8888`
  - depends_on：eureka-server

- `gateway`
  - 端口：`8080:8080`
  - depends_on：eureka-server、config-server

### 3.2 业务服务（全部注册到 Eureka，并依赖 MySQL）

- `user-service`
  - 职责：用户注册/登录/JWT 发行与用户信息（详见 `docs/user-service.md`）

- `merchant-service`
  - 职责：商家与店铺审核/入驻申请（详见 `docs/merchant-service.md`）

- `product-service`
  - 职责：菜品/商品（Food/Product）维护与查询（详见 `docs/product-service.md`）

- `cart-service`
  - 职责：购物车增删改查（详见 `docs/cart-service.md`）

- `order-service`
  - 职责：订单创建/查询/状态流转（详见 `docs/order-service.md`）

- `address-service`
  - 职责：收货地址维护（`address-service.md` 待补齐可再扩展）

- `points-service`
  - 职责：积分

- `wallet-service`
  - 职责：钱包/余额

> 说明：上面业务服务在 compose 里 **没有对外暴露端口**（都在同一 docker network 内互通）；外部访问基本都经由 `gateway:8080`。

### 3.3 前端

- `frontend`
  - build context：`./elm-frontend`
  - container_name：`elm-frontend-1`
  - 端口：`80:80`
  - depends_on：gateway

## 4. 典型请求链路（联调时最实用）

以「用户下单」为例的链路（概念级）：

1. 浏览器访问 `http://localhost` 进入前端
2. 前端请求 `http://localhost:8080/...` 进入 Gateway
3. Gateway 通过 Eureka 找到对应后端服务（如 `user-service`、`product-service`、`cart-service`、`order-service`）
4. 服务读写 MySQL（通过初始化脚本创建/填充 schema 和种子数据）

## 5. 约定与易踩坑（和现状保持一致）

- **构建上下文**：各服务 Dockerfile 通常以仓库根或 `elm-cloud` 根为 build context，避免 `Non-resolvable parent POM`。
- **数据库初始化**：首次启动耗时主要在拉镜像/构建 jar/执行 MySQL init。
- **服务端口信息**：README 中列了默认端口，但最终以各服务 `application*.yml/properties` 为准；compose 目前只显式映射了 eureka/config/gateway/mysql/frontend。

## 6. 推荐你下一步补齐的文档点（低风险增量）

- 为 `address-service`、`points-service`、`wallet-service` 补齐与其它 docs 同风格的说明
- 在 `gateway` 的配置里补一份「路由表」概览（哪些 Path 进哪些 service）
- 在 `docker/mysql/init` 中注释说明每个 schema 与哪个服务对应
