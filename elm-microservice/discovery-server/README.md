# discovery-server

## 业务边界

`discovery-server` 提供 Eureka 注册发现能力，不承载业务数据。

- 服务注册
- 服务发现
- 为 Gateway 和各客户端提供实例列表

## 当前实现说明

- 当前采用双节点口径运行：`discovery-server-a`、`discovery-server-b`
- 核心业务服务通过服务名而不是固定地址访问下游
- 网关配置刷新入口在枚举实例时依赖本服务提供的注册信息

## Docker 部署（统一方式）

```bash
docker compose up -d --build discovery-server-a discovery-server-b
```

默认入口：`8761`

## 运行配置

- `CONFIG_SERVER_URI`
- `EUREKA_DEFAULT_ZONE`

## 验收说明

- 课程验收重点不是页面，而是能在 Eureka 中看到同一 `serviceId` 下多实例注册
- 如果 `gateway-service` 的配置刷新直接返回结构化 `503`，应优先检查本服务是否可用