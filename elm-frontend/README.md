# ELM 前端项目

基于 Vue 3 + TypeScript + Vite + Element Plus 构建的外卖平台前端。

## 快速开始

### Docker部署（推荐）

在项目根目录执行：

```bash
docker-compose up -d
```

访问：http://localhost

### 本地开发

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
