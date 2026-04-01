# ELM 前端项目

基于 Vue 3 + TypeScript + Vite + Element Plus 构建的新版前端，当前默认通过 `gateway-service` 接入 Spring Cloud 后端拓扑。

## 当前口径

- 当前主入口是仓库根目录的 `docker-compose.yml`，前端容器通过 Nginx 访问 `gateway-service`
- 本地开发时，Vite dev server 默认把 `/api` 代理到 `http://localhost:8090`
- 当前前端自动化测试基线为 `13` 个测试文件、`54` 个测试全绿
- 与微服务实现、验收说明配套阅读时，优先参考根目录 `README.md` 与 `docs/` 下的说明文档

## 文档入口

- 根仓库总说明：`../README.md`
- 微服务实现说明：`../docs/microservice-implementation-guide.md`
- 后端测试基线：`../docs/backend-test-baseline.md`
- 前端设计参考：`docs/vuedesign.md`
- 前端开发计划参考：`docs/vueplan.md`
- 前端需求分析参考：`docs/vueSRS.md`

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
- 聚合 API：`http://localhost:8080/elm`

说明：

- 当前部署方案只使用根目录 `docker-compose.yml`
- 前端容器通过 Nginx 代理到 `gateway-service`，浏览器侧不直接感知各个后端微服务地址
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

默认情况下，Vite 会把 `/api` 请求代理到 `http://localhost:8090`；如需改目标，可通过环境变量 `VITE_API_PROXY_TARGET` 覆盖。

3. 构建生产版本：

```bash
pnpm build
```

4. 运行自动化测试：

```bash
pnpm test:run
```

当前已覆盖认证、路由守卫、组件和关键页面交互，最近一次结果为 `13 files / 54 tests` 全绿。

## 技术栈

- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia (状态管理)
- Vue Router
- Axios
- Vitest + Vue Test Utils

## 项目结构

- `src/api/` - API接口定义
- `src/components/` - 公共组件
- `src/views/` - 页面组件
- `src/store/` - Pinia状态管理
- `src/router/` - 路由配置

## 当前实现说明

- 前端默认通过 `gateway-service` 统一访问后端，不直接面向各微服务地址开发
- 外部业务接口主要走 `/api/** -> gateway-service -> elm-v2.0` 的聚合链路
- 服务发现、负载均衡、配置刷新等微服务高层能力由后端和网关承担，前端主要负责统一入口访问和降级提示
