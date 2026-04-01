# points-service

## 业务边界

`points-service` 只负责积分域能力。

- 积分账户/余额
- 积分交易（冻结、扣减、返还）
- 积分规则
- 内部接口前缀：`/elm/api/inner/points/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用（RestTemplate）
- 典型调用点：下单扣积分、取消返还积分、订单完成/评价积分发放

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
# 在仓库根目录执行
docker compose up -d --build points-service
```

默认端口：`8081`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_points`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 单独构建镜像

```bash
docker build -t elm/points-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
