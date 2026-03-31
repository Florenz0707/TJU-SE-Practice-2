## 项目介绍

`elm-v2.0` 是新版方案里的外部聚合层，负责对前端提供统一 API、鉴权上下文、兼容响应结构以及跨域编排；实际业务能力由根目录 `docker-compose.yml` 编排的微服务集群承载。

当前仓库的标准前后端组合为：

- 聚合层：`elm-v2.0`
- 网关：`elm-microservice/gateway-service`
- 前端：`../elm-frontend`

## 推荐启动方式

### 1. 根目录统一容器编排

在仓库根目录执行：

```bash
cp .env.example .env
docker compose up -d --build
```

默认访问入口：

- Spring Cloud Gateway：`http://localhost:8090`
- 聚合层 Swagger：`http://localhost:8080/swagger-ui/index.html`
- 聚合层 API 基地址：`http://localhost:8080/elm`

如需同时启动前端容器：

```bash
docker compose --profile frontend up -d --build
```

### 2. 本地云模式联调

当环境没有 Docker 时，优先使用仓库根目录脚本启动 Spring Cloud 基础设施和后端服务。

启动配置中心、注册中心、网关：

```bash
bash scripts/run-local-cloud.sh
```

启动业务服务与聚合层：

```bash
bash scripts/run-local-backend-cloud.sh
```

停止：

```bash
bash scripts/stop-local-backend.sh
bash scripts/stop-local-cloud.sh
```

## `elm-v2.0` 单模块本地运行

仅在下列场景下建议单独启动本模块：

- 调试聚合层接口兼容逻辑
- 联调已有运行中的下游微服务
- 查看 Swagger 或排查聚合层日志

启动前需要保证 `account-service`、`points-service`、`business-service`、`food-service`、`cart-service`、`order-service`、`address-service`、`user-service` 已经可达。

```bash
export JAVA_HOME=/root/workspace/TJU-SE-Practice-2/.tools/jdk-21
export PATH="$JAVA_HOME/bin:/root/workspace/TJU-SE-Practice-2/.tools/apache-maven-3.9.9/bin:$PATH"
mvn -Dmaven.test.skip=true -Dspring-boot.run.profiles=local,cloud spring-boot:run
```

说明：

- 本仓库当前在容器外联调时应优先复用 `.tools/jdk-21` 和 `.tools/apache-maven-3.9.9`
- `mvn spring-boot:run` 默认会走测试编译；若只是恢复联调环境，建议加 `-Dmaven.test.skip=true`

## 联调与 Smoke

业务链路 smoke 脚本位于 `elm-v2.0/scripts/`。

准备：

```bash
cd scripts
cp integration.env.example .env
uv sync
```

执行四服务 smoke：

```bash
uv run run_four_service_smoke.py --env-file .env
```

常用模式：

- 仅对已启动环境做联调验证：`uv run run_four_service_smoke.py --env-file .env --skip-start`
- 执行账户灰度演练：`uv run run_phase3_account_drill.py --env-file .env`

## 近期验证结论

- 2026-03-31 已在非 compose 本地直跑模式下验证通过：注册、登录、`/api/wallet/my/topup`、下单、取消、完成、评价增删查、我的订单查询
- 聚合层 `/api/wallet` 已与 `account-service` 资金源收敛；钱包充值后可直接通过 `/api/orders` 完成支付
- 新注册用户在当前链路下默认已存在钱包，通常表现为 `walletId=userId`

## 环境变量

常用变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CONFIG_SERVER_URL`
- `EUREKA_SERVER_URL`
- `INTERNAL_SERVICE_TOKEN`

容器统一部署时，优先使用仓库根目录 `.env`；业务 smoke 脚本使用 `elm-v2.0/scripts/.env`。

## 接口与测试

- Swagger：`http://localhost:8080/swagger-ui/index.html`
- Apifox 项目：`https://tjusep.apifox.cn/`

如果只想验证新版主链路，优先使用：

- 网关入口 `http://localhost:8090`
- 前端 Vite 开发环境 `elm-frontend`
- `elm-v2.0/scripts/run_four_service_smoke.py`
