# elm-cloud 常见问题与排错

本页聚焦 **Docker Compose 本地联调** 的高频问题，尽量给到“现象 → 原因 → 处理方式”。

## 1. 服务起不来 / 一直重启（Restarting）

### 1.1 先确认整体状态

在 `elm-cloud` 目录下：

```powershell
docker compose ps
```

- 看 `STATUS` 是否是 `Up`。
- 若是 `Restarting`，直接看该服务日志：

```powershell
docker compose logs -f <service-name>
```

### 1.2 MySQL 还没就绪导致后端服务启动失败

**现象**：业务服务日志显示数据库连接失败（`Communications link failure` / `Connection refused`）。

**原因**：compose 的 `depends_on` 只保证“启动顺序”，不保证 MySQL 已完成初始化。

**处理**：
- 等待 MySQL 初始化完成（首次启动会执行 `docker/mysql/init`，可能稍慢）。
- 观察 MySQL 日志直到出现类似 “ready for connections”。

```powershell
docker compose logs -f mysql
```

## 2. Docker build 报 parent POM 找不到

**现象**：`Non-resolvable parent POM`。

**原因**：多模块 Maven 构建时，Docker build 上下文缺少父 POM 或模块间依赖。

**处理**：
- 本项目已采用“以仓库/模块根作为 build context”的方式解决；如果你自改了 Dockerfile，请确保 Docker build context 能包含 `elm-cloud/pom.xml` 以及必要模块。

## 3. 前端能打开但接口 4xx/5xx

### 3.1 优先确认 Gateway 与 Eureka

- Eureka：http://localhost:8761
  - 看 Instances 是否注册了 `GATEWAY` 以及各业务服务。
- Gateway：http://localhost:8080

如果业务服务没注册上来：
- 去看对应业务服务日志（通常是配置/数据库/端口冲突）。

### 3.2 路由不匹配

**现象**：Gateway 返回 404。

**处理思路**：
- 对照 `gateway` 的路由配置（通常在其 `application.yml` 或从 Config Server 加载）。
- 确认前端请求前缀是否和路由一致（比如 `/elm/api/**`）。

## 4. 想“重置数据库”

**目标**：清空所有数据，重新执行 init 脚本。

```powershell
docker compose down -v
```

然后重新启动：

```powershell
docker compose up -d --build
```

> 注意：`-v` 会删除 `mysql-data` 数据卷，等价于“删库重来”。

## 5. 端口被占用

**现象**：启动时报 `bind: address already in use`。

**处理**：
- 先找出占用端口的进程并停止；或修改 `docker-compose.yml` 的宿主机映射端口。
- 当前 compose 显式映射的端口有：3306、8761、8888、8080、80。
