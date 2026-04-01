# food-service

## 业务边界

`food-service` 负责菜品与库存域。

- 菜品查询与快照
- 库存预占 / 回补
- 批量库存一致性与并发保护
- 内部接口前缀：`/elm/api/inner/food/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用，用于下单前菜品校验、库存预占和取消订单回补
- 通过 OpenFeign 调用 `business-service`，并使用 Resilience4j 处理下游容错

## Docker 部署（统一方式）

```bash
docker compose up -d --build food-service-a food-service-b
```

默认端口：`8087`、`8187`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_catalog`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 当前服务已补齐库存预占/回补、失败 requestId 重放、批量一致性和并发保护测试
- Spring Cloud LoadBalancer 的 Caffeine cache 依赖已补齐，测试输出中不再出现 `LoadBalancerCaffeineWarnLogger`