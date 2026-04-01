# order-service

## 业务边界

`order-service` 只负责订单域核心数据，不直接处理资金/积分/库存扣减策略。

- 订单主表、订单明细
- 地址
- 购物车
- 评价
- 内部接口前缀：`/elm/api/inner/order/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用（RestTemplate）
- 典型调用点：创建/取消订单、状态推进、地址/购物车/评价读写

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
# 在仓库根目录执行
docker compose up -d --build order-service
```

默认端口：`8084`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_order`）
- `DB_USERNAME`
- `DB_PASSWORD`

## 单独构建镜像

```bash
docker build -t elm/order-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
