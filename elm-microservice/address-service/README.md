# address-service

## 业务边界

`address-service` 负责配送地址域的独立存储与读写。

- 配送地址创建、查询、更新、删除
- 内部接口前缀：`/elm/api/inner/address/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用，用于前台地址管理与下单时的地址校验
- 当前保持单实例部署，符合课程中地址服务不集群化的要求

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
docker compose up -d --build address-service
```

默认端口：`8085`
默认上下文：`/elm`