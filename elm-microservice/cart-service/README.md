# cart-service

## 业务边界

`cart-service` 负责购物车域。

- 购物车增删改查
- 下单前购物车读取
- 下单后购物车清空
- 内部接口前缀：`/elm/api/inner/cart/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用，用于用户购物车页面和下单编排
- 当前服务以双实例方式运行，是课程验收中的核心集群服务之一

## Docker 部署（统一方式）

```bash
docker compose up -d --build cart-service-a cart-service-b
```

默认端口：`8089`、`8189`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_cart`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 旧的订单域内购物车实现已经删除，购物车职责只保留在本服务
- 自动化测试已覆盖购物车创建、数量更新、按用户查询和删除等核心分支