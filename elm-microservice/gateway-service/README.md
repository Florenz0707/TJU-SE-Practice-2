# gateway-service

## 业务边界

`gateway-service` 是 Spring Cloud Gateway 网关，不承载业务数据。

- 对外暴露统一入口
- `/api/**` 转发到 `elm-v2.0`
- `/services/*/**` 直通各微服务，便于验收现场展示真实拆分入口
- 提供统一配置刷新入口：`POST /internal/config/refresh`

## 与其他服务关系

- 通过 Eureka 做服务发现
- compose 验收口径下，上游路由统一使用 `lb://service-id`
- 配置刷新入口会枚举注册实例并聚合各实例的刷新结果

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
docker compose up -d --build gateway-service
```

默认端口：`8090`

## 运行配置

- `CONFIG_SERVER_URI`
- `EUREKA_DEFAULT_ZONE`
- `CONFIG_REFRESH_TOKEN`

## 当前实现说明

- 已支持服务发现整体失败时返回结构化 `503`
- 已支持单个服务实例发现失败时继续返回聚合失败结果，而不是直接让整个刷新接口报错
- 自动化测试已覆盖配置刷新聚合、路由 rewrite、OpenAPI 路由和 user-service 直连路由