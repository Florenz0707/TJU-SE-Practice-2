# catalog-service（历史拆分中间态）

## 业务边界

`catalog-service` 是早期“目录域合并服务”的中间态实现，当前主线部署已经改为 `business-service + food-service` 拆分模式。

- 当前仓库保留它主要用于历史演进说明，不作为新版 Spring Cloud 验收拓扑的主服务

## 与其他服务关系

- 当前新版主链路中，目录域能力已经拆到：
- `business-service`：商家查询与快照
- `food-service`：菜品查询、库存预占 / 回补、商家归属校验

## Docker 部署（统一方式）

当前根目录 `docker-compose.yml` 不再部署本服务；新版部署请使用 `business-service` 和 `food-service`。

```bash
# 新版目录域部署方式
docker compose up -d --build business-service-a business-service-b food-service-a food-service-b
```

## 运行配置

- 若只做历史代码研究，可按模块自身代码手动运行
- 课程验收与当前联调请忽略本模块，统一以 `business-service` / `food-service` 为准

## 单独构建镜像

```bash
docker build -t elm/catalog-service:local .
```

Dockerfile：[`Dockerfile`](./Dockerfile)
