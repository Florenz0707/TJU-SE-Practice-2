# 本地运行指南（elm-cloud）

本文档指导在本地使用 Docker Compose 快速启动 elm-cloud 下的所有微服务、前端和数据库。

## 1. 准备条件

- 请确保本机已安装并能运行 **Docker Desktop**（或 Docker Engine + Compose v2+）。
- 所有操作均在 elm-cloud 服务目录下进行。
- 确保 80、8080、8761、3306 端口未被占用。

## 2. 快速启动（一键运行所有功能）

目前所有后端微服务（包括 gateway、merchant-service、user-service 等），以及前端应用（frontend）和数据库环境（mysql），都已统一配置在了 elm-cloud/docker-compose.yml 中。实际构建代码均与当前配置完全匹配，支持直接启动。

如果自动拉取镜像失败可以手动拉取

```PowerShell
# 1. MySQL 数据库镜像
docker pull mysql:8.0

# 2. Java 运行环境镜像（所有 Java 微服务都会用这个）
docker pull eclipse-temurin:21-jre

# 3. Node.js 镜像（前端构建用）
docker pull node:22-alpine

# 4. Nginx 镜像（前端生产环境用）
docker pull nginx:stable-alpine
```
1. **进入elm-cloud目录**：
```powershell
cd elm-cloud
```
1. **清理旧容器和数据（如果之前运行过）**：

```powershell
docker compose down -v
```

1. **构建并后台运行所有容器**：

```powershell
# Compose v2（推荐）
docker compose up -d --build
```

> **注：** 首次启动时，为了拉取基础镜像并进行 Java Maven 包和前端 Node 环境的构建，可能会消耗较长的时间（约 10-20 分钟），请耐心等待。

## 3. 验证服务启动

服务启动后，请按以下顺序验证：

1. **等待所有服务健康启动**（约需 3-5 分钟）：
   ```powershell
   docker compose ps
   ```
   确保所有服务的 STATUS 都是 Up（healthy）
2. **查看 Eureka 注册中心**：
   浏览器访问：<http://localhost:8761>
   应能看到所有微服务都已注册
3. **查看网关**：
   浏览器访问：<http://localhost:8080>
   应能看到网关响应

## 4. 访问系统与测试账号

服务成功启动后即可在本地访问：

### 前端系统入口

- **前端系统**：直接在浏览器访问 <http://localhost>

### 测试账号信息

数据库初始化脚本已预置以下账号，可直接使用：

| 账号类型      | 用户名       | 密码     |
| --------- | --------- | ------ |
| **管理员**   | admin     | admin  |
| **商家1**   | business1 | 111111 |
| **商家2**   | business2 | 111111 |
| **普通用户1** | user1     | 111111 |
| **普通用户2** | user2     | 111111 |

## 5. 常用维护命令

如果你想单独查看某个服务的状态，或者更新单独的服务，可以使用以下命令（均确保您位于 elm-cloud 目录下执行）：

### 查看服务状态和日志

- **查看所有服务状态**：
  ```powershell
  docker compose ps
  ```
- **查看服务日志**（例如查看 user-service 或 gateway 的运行日志）：
  ```powershell
  # 查看所有服务日志
  docker compose logs -f

  # 查看特定服务日志
  docker compose logs -f user-service
  docker compose logs -f gateway
  docker compose logs -f order-service
  docker compose logs -f cart-service
  ```

### 重新构建和重启服务

- **单独重新构建某一项服务**（如修改了代码后期望单独编译更新）：
  ```powershell
  # 停止并删除旧容器
  docker compose stop merchant-service
  docker compose rm -f merchant-service

  # 重新构建并启动
  docker compose up -d --build merchant-service
  ```
- **重启某个服务**：
  ```powershell
  docker compose restart merchant-service
  ```

### 清理和重置

- **停止所有服务但保留数据**：
  ```powershell
  docker compose stop
  ```
- **停止并删除所有服务与数据卷**（彻底重置系统数据和数据库）：
  ```powershell
  docker compose down -v
  ```
  *(清除后下次 up 时会重新触发 MySQL 数据的初始化)*

## 6. 服务端口映射

| 服务            | 外部端口 | 内部端口 | 说明        |
| ------------- | ---- | ---- | --------- |
| frontend      | 80   | 80   | 前端 Nginx  |
| gateway       | 8080 | 8080 | API 网关    |
| eureka-server | 8761 | 8761 | 服务注册中心    |
| mysql         | 3306 | 3306 | MySQL 数据库 |

## 7. 数据库初始化说明

数据库初始化脚本位于 `docker/mysql/init/` 目录：

- **01-create-schemas.sql**：创建所有数据库和基础表结构
- **02-initialize-data.sql**：初始化测试数据（用户、商家、菜品、地址等）

MySQL 会在首次启动时自动执行这些脚本。如果需要重新初始化，执行 `docker compose down -v` 后再启动即可。

## 8. 故障排查

### 服务启动失败

如果某个服务启动失败或频繁重启：

1. 查看该服务的日志：
   ```powershell
   docker compose logs -f <service-name>
   ```
2. 检查端口是否被占用：
   ```powershell
   netstat -ano | findstr ":80"
   netstat -ano | findstr ":8080"
   ```
3. 确保 Docker 有足够的内存（建议至少 4GB）

### 数据库连接失败

- 确保 mysql 容器已启动：`docker compose ps`
- 查看 mysql 日志：`docker compose logs -f mysql`
- 等待 mysql 完全启动（约需 30-60 秒）

### 前端访问异常

- 确保 frontend 容器正在运行
- 检查浏览器控制台是否有错误
- 确认 gateway 服务正常运行

## 9. Config + Bus 自动演示脚本

如果你要做答辩现场演示，或者想快速回归验证 Config + Bus 链路，可以直接运行 `elm-cloud/scripts/` 下的脚本。

### 9.1 基础健康检查

```bash
bash scripts/check_config_bus_stack.sh
```

这个脚本会检查：

- Eureka 首页是否可用
- 两个 Config Server 的健康状态
- Gateway 健康状态
- Eureka 注册表中是否存在 `CONFIG-SERVER`、`GATEWAY`、`ORDER-SERVICE`
- `GET /elm/api/orders/runtime-config` 是否正常返回

### 9.2 一键动态刷新演示

```bash
bash scripts/demo_config_bus_refresh.sh
```

这个脚本会自动完成：

1. 读取当前 `runtime-config` 基线值
2. 修改 `config/order-service.yml` 中的 `demo.config.message` 和 `demo.config.version`
3. 校验 Config Server 已读到新值
4. 调用 `POST /actuator/busrefresh`
5. 轮询 `GET /elm/api/orders/runtime-config`，直到返回新值
6. 默认自动把配置恢复为演示前状态，并再次刷新

如果你想保留演示值，可以执行：

```bash
bash scripts/demo_config_bus_refresh.sh --keep-demo-value
```

也可以自定义演示值：

```bash
bash scripts/demo_config_bus_refresh.sh \
  --message "order-service remote config live demo custom" \
  --version "demo-custom"
```

### 9.3 强制恢复默认演示值

```bash
bash scripts/restore_order_demo_config.sh
```

这个脚本会把 `demo.config.message` 恢复成 `order-service remote config ready`，把 `demo.config.version` 恢复成 `v1`，并再次触发 Bus 刷新。

### 9.4 答辩专用一键串讲

```bash
./scripts/defense_config_bus_showcase.sh
```

这个脚本更适合投屏展示，默认会按步骤停顿，方便你一边讲一边演示。它会依次展示：

1. 当前关键容器状态
2. `gateway` 和 `order-service` 的 discovery-first 证据
3. Config Server 的 native / git 双模式证据
4. 刷新前的 `runtime-config` 返回值
5. 修改配置并调用 `busrefresh`
6. 刷新后的 `runtime-config` 返回值
7. 一段适合答辩收口的总结输出

如果你不想每一步都等待回车，可以执行：

```bash
./scripts/defense_config_bus_showcase.sh --auto
```

### 9.5 覆盖 task 要求的自动验收

```bash
./scripts/verify_task_requirements.sh
```

这个脚本会按 `task/` 里的四份要求文本分组验收：

- `配置中心.txt`
- `Config集中配置管理集群.txt`
- `Bus配置刷新.txt`
- `动态刷新配置.txt`

它同时做两类检查：

1. 静态检查：依赖、`bootstrap.yml`、Config Server、docker-compose、配置文件覆盖面
2. 动态检查：健康接口、Eureka 注册、`busrefresh` 调用、动态刷新端到端演示

脚本输出里：

- `PASS` 表示已自动证明
- `MANUAL` 表示脚本已经覆盖到这条要求，但该项本质上需要人工环境配合，例如“远程 Git 仓库真实性”

如果你希望只要存在 `MANUAL` 项也返回非 0，可以执行：

```bash
./scripts/verify_task_requirements.sh --strict-manual
```

