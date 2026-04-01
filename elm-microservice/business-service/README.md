# business-service

## 业务边界

`business-service` 负责商家域数据。

- 商家查询与快照
- 商家可用性与归属校验的上游数据源
- 内部接口前缀：`/elm/api/inner/business/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用，用于店铺查询和订单展示补齐
- 被 `food-service` 通过 OpenFeign 调用，用于商家存在性和归属校验

## Docker 部署（统一方式）

```bash
docker compose up -d --build business-service-a business-service-b
```

默认端口：`8083`、`8183`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_catalog`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 当前服务以双实例方式运行，是课程验收中的核心集群服务之一
- 自动化测试已覆盖商家快照列表、按 ID 查询和内部 controller 基础分支