# order-service

## 业务边界

`order-service` 只负责订单域核心数据，不直接处理资金/积分/库存扣减策略。

- 订单主表、订单明细
- 评价
- 内部接口前缀：`/elm/api/inner/order/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用（RestTemplate）
- 典型调用点：创建/取消订单、状态推进、评价读写
- 地址域和购物车域已经分别拆到 `address-service` 与 `cart-service`，本服务不再承载这两块数据

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
# 在仓库根目录执行
docker compose up -d --build order-service-a order-service-b
```

默认端口：`8084`、`8184`
默认上下文：`/elm`

如只需联调一个实例，也可以单独启动：

```bash
docker compose up -d --build order-service-a
```

## 运行配置

- `DB_URL`（建议指向 `elm_order`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 当前服务以双实例方式运行：`8084` / `8184`
- `elm-v2.0` 会通过内部接口创建订单快照、取消订单、推进状态，并在评价链路中读取和回写订单数据
- 自动化测试已覆盖 requestId 幂等、取消订单、状态流转和内部 controller 基础分支

## 单独构建镜像

```bash
docker build -t elm/order-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
