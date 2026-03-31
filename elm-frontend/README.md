# ELM 前端项目

基于 Vue 3 + TypeScript + Vite + Element Plus 构建的外卖平台前端。

## 快速开始

### Docker部署（推荐）

当前项目统一通过仓库根目录的 `docker compose` 编排启动，前端容器会自动接入 `gateway-service`，不需要在宿主机单独安装 `pnpm`。

在仓库根目录执行：

```bash
cp .env.example .env
docker compose up -d --build
```

访问入口：

- 前端首页：`http://localhost`
- 网关：`http://localhost:8090`
- Eureka：`http://localhost:8761`

说明：

- 当前部署方案只使用根目录 `docker-compose.yml`
- `elm-v1.0` 是历史代码，不参与当前容器化部署

### 本地开发

以下方式仅用于前端代码开发，不是当前环境的部署方式：

1. 安装依赖：

```bash
pnpm install
```

2. 启动开发服务器：

```bash
pnpm dev
```

3. 构建生产版本：

```bash
pnpm build
```

## 技术栈

- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia (状态管理)
- Vue Router
- Axios

## 项目结构

- `src/api/` - API接口定义
- `src/components/` - 公共组件
- `src/views/` - 页面组件
- `src/store/` - Pinia状态管理
- `src/router/` - 路由配置
