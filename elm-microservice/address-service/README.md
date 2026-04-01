# address-service

## 业务边界

`address-service` 负责配送地址域的独立存储与读写。

- 配送地址创建、查询、更新、删除
- 内部接口前缀：`/elm/api/inner/address/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用，用于前台地址管理与下单时的地址校验
- 当前保持单实例部署，符合课程中地址服务不集群化的要求
- 内部接口统一受 `X-Internal-Service-Token` 保护

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
docker compose up -d --build address-service
```

默认端口：`8085`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_address`）
- `DB_USERNAME`
- `DB_PASSWORD`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 当前地址域已经完全独立出 `order-service`，聚合层在地址管理和订单详情补齐时都会远程调用本服务
- 自动化测试已覆盖地址创建、更新、按用户查询和逻辑删除等核心分支