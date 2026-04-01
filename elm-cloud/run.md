# 本地运行指南（elm-cloud）

本文档给出在本地使用 Docker Compose 启动 `elm-cloud` 微服务的步骤（PowerShell）。

> 说明：此说明假设你已在本机安装并能运行 Docker Desktop（Docker Engine + Compose v2+），并且在仓库根目录为 `d:\partical\TJU-SE-Practice=2`。

## 1. 目录与前置条件

- 项目根目录（示例）：
  - `d:\partical\TJU-SE-Practice=2`（仓库根）
  - `elm-cloud`（本模块）
  - `elm-frontend`（可选前端项目，若希望通过 docker-compose 启动前端，需要把前端服务片段加入 compose）

## 2. 快速启动（建议先单模块验证）

1) 先在 repo 根打开 PowerShell：

```powershell
cd 'd:\partical\TJU-SE-Practice=2'
```

2) 单模块构建（验证 address-service）：

```powershell
# 仅构建 address-service 镜像（使用 elm-cloud/docker-compose.yml 的 build 配置）
docker compose -f .\elm-cloud\docker-compose.yml build address-service
```

如果构建成功，继续构建其它服务或直接启动全部。

3) 启动全部（首次可能需要较长时间）：

```powershell
# 在仓库根执行，强制构建并后台运行
docker compose -f .\elm-cloud\docker-compose.yml up --build -d
```

## 3. 常用命令（日志、停止、重建）

- 查看某个服务的日志（跟随输出）：

```powershell
docker compose -f .\elm-cloud\docker-compose.yml logs -f user-service
```

- 停止并删除容器（保留数据卷）：

```powershell
docker compose -f .\elm-cloud\docker-compose.yml down
```

- 停止并删除容器及卷（会触发 MySQL 重新初始化）：

```powershell
docker compose -f .\elm-cloud\docker-compose.yml down -v
```

- 只重建某个服务并重启：

```powershell
docker compose -f .\elm-cloud\docker-compose.yml build user-service
docker compose -f .\elm-cloud\docker-compose.yml up -d user-service
```

## 4. 前端（elm-frontend）说明

- 当前 `elm-cloud/docker-compose.yml` 默认不包含前端（前端在仓库根 `elm-frontend`）。如果你已经把原来的前端启动片段拷贝到 `elm-cloud/docker-compose.yml` 中，并确保 build context 指向前端目录，那么这样是可以的。示例（可加入到 `elm-cloud/docker-compose.yml`）：

```yaml
  frontend:
    build:
      context: ..
      dockerfile: ./elm-frontend/Dockerfile
    ports:
      - 8088:80
    depends_on:
      - gateway
```

或者你也可以在宿主机直接运行前端（更快）:

```powershell
cd 'd:\partical\TJU-SE-Practice=2\elm-frontend'
pnpm install
pnpm run dev
```

（若你使用 npm/yarn，请替换相应命令）

## 5. 常见故障与排查

- 父 POM 相关错误（Non-resolvable parent POM）：请确认你是从仓库根执行 `docker compose`，并且 `elm-cloud/*/Dockerfile` 已按本项目模式使用 repo 根作为构建上下文（已修改）。如仍报错，贴 build 日志我会帮你分析。
- 数据库权限或表未创建：查看服务日志（Hibernate 输出）；若 MySQL 用户权限不足，建议把必要的 CREATE DATABASE 语句放到 `docker/mysql/init`，并在启动前执行 `docker compose down -v` 以让 init 脚本重新执行。
- 构建很慢或依赖下载失败：建议先在宿主机用 Maven 构建：

```powershell
mvn -T1C -DskipTests package
```

然后再运行 `docker compose -f .\elm-cloud\docker-compose.yml build`（或修改 Dockerfile 让其直接复制 host 上的 jar，详见 README 中的“构建加速”一节）。

## 6. 我可以为你进一步做的事（可选）

- 在仓库根添加 `.dockerignore`（减少构建上下文体积）。
- 添加一个 `Dockerfile.hostbuild`（仅复制宿主机构建的 jar），并把运行步骤写成脚本以便快速重建镜像。
- 把前端服务直接加入 `elm-cloud/docker-compose.yml`（如果你确认要把前端也用该 compose 管理）。

如果你希望我直接把前端片段加入 `elm-cloud/docker-compose.yml` 或帮助添加 `.dockerignore`，请回复确认，我会在 `elm-cloud` 下完成并提交更改。
