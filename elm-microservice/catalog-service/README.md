# catalog-service

## 业务边界

`catalog-service` 只负责目录与库存域，不处理订单或资金。

- 商家查询
- 菜品查询
- 库存预占 / 库存回补（幂等）
- 内部接口前缀：`/elm/api/inner/catalog/**`

## 与其他服务关系

- 被 `elm-v2.0` 调用（RestTemplate）
- 典型调用点：下单前价格/库存校验、下单库存预占、取消库存回补

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
# 在仓库根目录执行
docker compose up -d --build catalog-service
```

默认端口：`8083`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（建议指向 `elm_catalog`）
- `DB_USERNAME`
- `DB_PASSWORD`

## 单独构建镜像

```bash
docker build -t elm/catalog-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
