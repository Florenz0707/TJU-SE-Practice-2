# config-server

## 业务边界

`config-server` 是 Spring Cloud Config Server，不承载业务数据。

- 为各微服务提供集中配置
- 支持 local/cloud 场景下的统一配置下发
- 为配置刷新链路提供配置源

## 当前实现说明

- 当前采用 `native` 模式，配置源来自仓库本地目录，而不是远程 Git 仓
- 这样更适合课程验收和离线演示，但能力上仍然属于集中配置管理
- 当前 compose 与本地 cloud 模式都以本服务作为启动链第一环

## Docker 部署（统一方式）

```bash
docker compose up -d --build config-server
```

默认端口：`8888`

## 运行配置

- `SPRING_PROFILES_ACTIVE`
- `CONFIG_REPO_PATH`（如有覆盖）

## 验收说明

- 建议先检查 `http://localhost:8888/actuator/health`
- 配置刷新由 `gateway-service` 统一触发，本服务本身不负责广播