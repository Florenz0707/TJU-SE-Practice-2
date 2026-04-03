# 本地运行指南（elm-cloud）

本文档指导在本地使用 Docker Compose 快速启动 elm-cloud 下的所有微服务、前端和数据库。

## 1. 准备条件

- 请确保本机已安装并能运行 **Docker Desktop**（或 Docker Engine + Compose v2+）。
- 所有操作均在 elm-cloud 服务目录下进行。

## 2. 快速启动（一键运行所有功能）

目前所有后端微服务（包括 gateway、merchant-service、user-service 等），以及前端应用（frontend）和数据库环境（mysql），都已统一配置在了 elm-cloud/docker-compose.yml 中。实际构建代码均与当前配置完全匹配，支持直接启动。

1) **进入** elm-cloud **目录**：
   `powershell
   cd elm-cloud
   `

2) **构建并后台运行所有容器**：
   `powershell
   docker-compose up -d --build
   `
   > **注：** 首次启动时，为了拉取基础镜像并进行 Java Maven 包和前端 Node 环境的构建，可能会消耗较长的时间，请耐心等待。

## 3. 访问系统与管理员账号

服务成功启动后即可在本地访问：

- **前端系统入口**：直接在浏览器访问 [http://localhost](http://localhost) （由于前端容器映射到了外部 80 端口）。
- **管理员账号（Admin）**：
  数据库初始脚本 docker/mysql/init/01-create-schemas.sql 中已为您预置了具备高级管理权限的账号，用于审核商家申请和管理配置系统：
  - **账号用户名**：dmin
  - **登录密码**：password

*(注：系统中的其它注册用户或演示账号，如无单独说明均可用相同的方式进行登录注册测试)*

## 4. 常用维护命令

如果你想单独查看某个服务的状态，或者更新单独的服务，可以使用以下命令（均确保您位于 elm-cloud 目录下执行）：

- **查看服务日志**（例如查看 user-service 或 gateway 的运行日志）：
  `powershell
  docker-compose logs -f user-service
  docker-compose logs -f gateway
  `

- **单独重新构建某一项服务**（如修改了 merchant-service 代码后期望单独编译更新）：
  `powershell
  docker-compose up -d --build merchant-service
  `

- **停止并删除所有服务与数据卷**（如果您希望彻底重置系统数据和数据库）：
  `powershell
  docker-compose down -v
  `
  *(清除后下次 up 时会重新触发 MySQL 数据的初始化)*

