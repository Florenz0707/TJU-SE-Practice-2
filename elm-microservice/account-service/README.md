# account-service

## 业务边界

`account-service` 只负责资金与券域，不处理订单主状态。

- 钱包：余额查询、扣款、退款
- 交易：交易流水记录与查询
- 券：核销、回滚
- 内部接口前缀：`/elm/api/inner/account/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用（RestTemplate）
- 典型调用点：下单扣款、取消退款、券核销/回滚
- 内部接口统一受 `X-Internal-Service-Token` 保护，`/api/inner/account/**` 缺失或错误 token 会返回 `401`

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
# 在仓库根目录执行
docker compose up -d --build account-service
```

默认端口：`8082`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_account`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 当前服务已经纳入根目录 Spring Cloud 运行拓扑，由 `config-server` 下发配置、向 Eureka 注册
- 对外演示优先通过 `gateway-service -> elm-v2.0` 完成，不建议直接让前端访问本服务
- 自动化测试已覆盖账户内部接口和内部 token 过滤链的核心路径

## 单独构建镜像

```bash
docker build -t elm/account-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
