# elm-cloud 微服务说明（简体中文）

本文档介绍 `elm-cloud` 模块的拆分思路、各个微服务的职责与运行要点，方便开发与本地调试。

## 一、总体架构

- 技术栈：Java 21 + Spring Boot 3.x + Spring Cloud（Eureka、Config Server、Gateway）
- 持久层：MySQL（每个微服务独立数据库 schema）
- 服务间调用：RestTemplate（@LoadBalanced）
- 认证：基于 JWT，保留原单体项目的认证设计（token 生成与校验逻辑在 user-service / security 模块）
- 容器化：每个服务提供 Dockerfile，组合在 `elm-cloud/docker-compose.yml` 中进行本地一键启动

架构（文字版）：

Eureka（注册中心） ←→ Config Server（配置中心）
           ↑
         Gateway（API 网关）
           ↑
  各业务微服务（user/order/cart/product/merchant/address/points/wallet）

## 二、拆分原则与实现要点

- 单一职责：把用户、订单、商品、购物车、商家、地址、积分、钱包等域分别拆成独立服务。
- 数据库隔离：每个服务使用独立 schema（例如 `elm_user`、`elm_order`、`elm_catalog` 等），服务在启动时连接到自己的 schema。
- 配置中心：`config-server` 以 native 模式示例化（从 classpath:/config 读取），可改为 Git 模式以便共享配置。
- 服务发现：所有微服务注册到 `eureka-server`，gateway 通过服务发现路由到后端服务。
- 自动建表：当前服务采用 `spring.jpa.hibernate.ddl-auto=update`，在服务首次启动时由 Hibernate 自动创建/更新表结构（适合开发测试）。
- 构建策略：为了解决 Maven 多模块在 Docker 构建时找不到父 POM 的问题，模块 Dockerfile 使用仓库根作为构建上下文并只构建目标模块（见 Dockerfile 中的 mvn -f pom.xml -pl ... -am 用法）。

## 三、微服务清单（模块名 / 默认端口 / 默认 DB schema）

- eureka-server — 8761（注册中心）
- config-server — 8888（配置中心）
- gateway — 8080（API 网关）
- user-service — 8082（用户，DB: elm_user）
- product-service — 8083（商品/目录，DB: elm_catalog）
- order-service — 8084（订单，DB: elm_order）
- merchant-service — 8085（商家，DB: elm_merchant）
- cart-service — 8086（购物车，通常与订单库或独立库协作，当前配置为 elm_order）
- address-service — 8087（地址，DB: elm_address）
- wallet-service — 8088（钱包，DB: elm_wallet）
- points-service — 8081（积分，DB: elm_points）

> 端口与 DB 名称以 `elm-cloud/*/src/main/resources/application.properties` 中设置为准，可通过 docker-compose 环境变量覆盖。

## 四、开发与运行（要点）

- 仓库采用 Maven 多模块（parent POM 在 `elm-cloud/pom.xml`）。为了在 Docker 中构建模块，Dockerfile 使用仓库根作为构建上下文并用 `mvn -f pom.xml -pl <module> -am` 指令只构建目标模块。
- 服务依赖 MySQL：`elm-cloud/docker-compose.yml` 中带有 `mysql` 服务（image: mysql:8.0），默认创建用户 `elm`，密码 `elm`。注意：若使用 `createDatabaseIfNotExist=true`，连接用户需要具备创建数据库的权限；否则请在 MySQL init 脚本中预先创建数据库。

## 五、注意事项与建议（开发环境）

1. 自动建库/建表权限：服务通过 JDBC URL 的 `createDatabaseIfNotExist=true` 与 `spring.jpa.hibernate.ddl-auto=update` 配合可以在首次运行时创建 schema 与表，但这依赖于数据库用户权限。生产环境建议使用 Flyway 或 Liquibase 管理 schema。 
2. 构建加速：把 `.dockerignore` 放到仓库根以排除 target、.git、node_modules 等可显著降低 Docker build 上下文体积；或者在本地先运行 `mvn package` 再让 Dockerfile 仅复制 jar（host-build 流程）以加速镜像构建。
3. Config Server：当前实现为 native 模式（从镜像内 classpath:/config 读取），适合本地开发；若需要集中管理，建议改为 Git 模式并把配置放到远程仓库。

## 六、常见问题（FAQ）

- 问：Docker build 报 `Non-resolvable parent POM`？
  答：这是因为构建上下文中缺少父 POM。解决方法是使用仓库根作为构建上下文或在容器内 COPY 整个仓库（本项目的 Dockerfile 已按此方式修改）。

- 问：表没有创建？
  答：请检查连接的 MySQL 用户是否有建库权限；查看服务日志中 Hibernate 的输出（是否打印建表 SQL）。必要时先在 `docker/mysql/init` 加上 CREATE DATABASE 语句，或允许服务以有权限的用户连接。

---

如需我把 `.dockerignore`、示例 frontend 的 compose 片段或 host-build 的 Dockerfile 模板加入仓库，请回复同意，我会在 `elm-cloud` 下添加相应文件或补充说明。

*** 此 README 只针对 `elm-cloud` 目录下的微服务拆分与运行说明。

## 文档导航

- 汇报用总文档：`docs/report-microservice-architecture.md`
  - 覆盖：单体（elm-v2.0）→ 微服务（elm-cloud）拆分、Eureka/Gateway/负载均衡（Ribbon 概念对照 LoadBalancer）、Config、以及 Bus/Hystrix 的现状与演进方案。
- 项目结构总览：`docs/project-structure.md`
- 本地运行指南：`run.md`
- 常见问题排错：`docs/troubleshooting.md`
# elm-cloud

This folder is a starter multi-module Spring Cloud project for the course.

Structure:

- `eureka-server` - service registry
- `config-server` - centralized config (skeleton)
- `gateway` - Spring Cloud Gateway
- `merchant-service`, `product-service`, `cart-service`, `order-service`, `address-service`, `user-service`, `points-service`, `wallet-service` - business services

How to build and run (local Docker):

1. From repository root run: `docker compose -f elm-cloud/docker-compose.yml up --build`

Notes:
- Config server currently uses local filesystem by default; if you want Git-backed config, add Git repo credentials to `config-server` application properties.
- Ports: Eureka 8761, Config 8888, Gateway 8080, MySQL 3306
