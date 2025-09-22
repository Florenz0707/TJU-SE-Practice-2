# 线上点餐平台前端设计文档

## 1. 引言

### 1.1. 项目概述

本项目旨在为线上点餐平台构建一个现代、高效且用户友好的前端应用程序。该平台将服务于三种核心用户角色：顾客（C端用户）、商家（B端用户）和平台管理员（A端用户）。前端将通过 RESTful API 与后端服务进行数据交互。

### 1.2. 文档目的

本设计文档旨在提供一个全面的前端技术方案和开发指南。它详细定义了项目的技术选型、架构设计、模块划分、组件策略和开发规范，以确保代码的质量、可维护性，并支持多名开发者并行开发。

### 1.3. 目标读者

本文档主要面向项目的前端开发人员、项目经理、UI/UX 设计师和测试工程师。

## 2. 技术栈与架构决策

为了构建一个高性能、可扩展的前端应用，我们选择以下技术栈：

| 类别            | 技术                    | 理由                                                         |
| --------------- | ----------------------- | ------------------------------------------------------------ |
| **核心框架**    | Vue 3 (Composition API) | 提供了更佳的逻辑组织方式、类型推导和性能，非常适合构建复杂应用。 |
| **构建工具**    | Vite                    | 提供极速的冷启动和热模块更新（HMR），显著提升开发体验。      |
| **状态管理**    | Pinia                   | Vue 官方推荐的新一代状态管理器，拥有简洁的 API、完善的 TypeScript 支持和模块化设计。 |
| **路由管理**    | Vue Router              | Vue 官方路由，与框架无缝集成，提供路由守卫等强大功能以实现认证和授权。 |
| **HTTP 客户端** | Axios                   | 成熟可靠的 HTTP 客户端，支持 Promise API，并提供拦截器功能，便于实现全局请求/响应处理。 |
| **UI 组件库**   | Element Plus            | 一套成熟且功能丰富的企业级 UI 组件库，可以大幅提升开发效率和界面一致性。 |
| **CSS 方案**    | Sass (SCSS)             | 提供变量、嵌套、混入等高级功能，便于组织和维护全局样式及主题。 |
| **代码规范**    | ESLint + Prettier       | 强制执行统一的代码风格和质量标准，减少潜在错误，提升代码可读性。 |
| **实时通信**    | WebSocket               | 根据需求文档，用于实现订单的实时追踪和商家仪表盘的即时订单通知。 |

## 3. 项目结构

项目将采用模块化的目录结构，将不同角色的应用逻辑清晰地分离开，同时共享通用的业务逻辑和组件。

```
src/
├── api/                # API 请求模块 (按资源划分)
│   ├── auth.js
│   ├── business.js
│   ├── user.js
│   ├── order.js
│   └── ...
├── assets/             # 静态资源 (图片, 字体)
├── components/         # 全局通用组件
│   ├── AppIcon.vue
│   ├── DataTable.vue
│   └── ...
├── router/             # 路由配置
│   ├── index.js        # 主路由文件
│   └── guards.js       # 路由守卫
├── store/              # Pinia 状态管理
│   ├── auth.js
│   ├── cart.js
│   └── user.js
├── styles/             # 全局样式与变量
│   ├── main.scss
│   └── _variables.scss
├── utils/              # 通用工具函数
│   ├── request.js      # Axios 实例与拦截器配置
│   └── index.js
├── views/              # 视图/页面组件
│   ├── customer/       # C 端 - 顾客应用
│   │   ├── Home.vue
│   │   ├── RestaurantDetail.vue
│   │   ├── Checkout.vue
│   │   └── Profile/
│   │       └── UserProfile.vue
│   ├── merchant/       # B 端 - 商家仪表盘
│   │   ├── Dashboard.vue
│   │   ├── MenuManagement.vue
│   │   └── OrderHistory.vue
│   └── admin/          # A 端 - 管理员后台
│       ├── UserManagement.vue
│       └── MerchantApproval.vue
├── App.vue             # 根组件
└── main.js             # 应用入口文件
```

## 4. 核心模块设计

### 4.1. API 层 (`/api` & `/utils/request.js`)

- **封装 Axios**: 在 `utils/request.js` 中创建 Axios 实例。设置基础 URL (`/api`) 和超时时间。
- **请求拦截器**:
  - 从 Pinia 的 `auth` store 中读取访问令牌 (Access Token)。
  - 如果令牌存在，则在每个请求的 `Authorization` header 中添加 `Bearer <token>`。
- **响应拦截器**:
  - **成功处理**: 直接返回 `response.data.data`，简化业务逻辑中的数据获取。
  - **错误处理**:
    - `401 Unauthorized`: 触发令牌刷新逻辑。若刷新失败，则清除本地认证信息并重定向到登录页。
    - **其他 4xx/5xx 错误**: 根据需求文档中的标准错误结构，提取 `message` 或 `error.message`，通过全局消息组件（如 ElMessage）向用户展示友好的错误提示。
- **API 模块化**: 在 `/api` 目录下，严格按照 `openapi.json` 中的 `tags` 对 API 进行模块化封装。例如，所有与用户相关的请求（`GET /api/users`, `POST /api/users`）都封装在 `api/user.js` 中，导出为具名函数（`getUserList()`, `createUser()`）。

### 4.2. 认证与授权 (`/store/auth.js` & `/router/guards.js`)

- **状态管理**: `store/auth.js` 将负责存储用户信息、角色和访问令牌。
  - `state`: `token` (内存存储), `user` (用户信息)。
  - `actions`:
    - `login(credentials)`: 调用 `api/auth.js` 中的登录接口，成功后将 `token` 存入 state。
    - `logout()`: 清除 state 中的 `token` 和 `user` 信息。
    - `fetchUserInfo()`: 登录后调用 `/api/user` 获取当前用户信息并存储。
- **路由守卫**: `router/guards.js` 将创建一个全局前置守卫 (`router.beforeEach`)。
  - 检查路由的 `meta` 字段中是否需要认证 (`requiresAuth`) 和所需角色 (`roles`)。
  - 如果需要认证，检查 `auth` store 中是否存在 `token`。若不存在，重定向到登录页。
  - 如果需要特定角色，检查 `auth` store 中用户的角色是否匹配。若不匹配，显示无权限页面。

### 4.3. 实时通信 (WebSocket)

- 将创建一个通用的 WebSocket 服务或 Vue Composition API `useWebSocket` hook。
- **顾客端**: 在订单追踪页面，连接 WebSocket 以接收 `order.status.updated` 等事件，并实时更新 UI。
- **商家端**: 在订单管理仪表盘，连接 WebSocket 以即时接收新订单通知（`order.new`），并发出声音和视觉提醒。

## 5. 应用模块 breakdown (并行开发计划)

项目主体将分为三个独立的子应用，可以由不同的开发者或团队并行开发。

### 5.1. 顾客应用 (`/views/customer`)

此模块面向终端消费者，UI/UX 必须友好、直观且具备响应式设计。

- **负责人**: Developer A
- **核心页面与组件**:
  - **首页 (`Home.vue`)**:
    - 功能: 餐厅列表展示、搜索、筛选。
    - API: `GET /api/businesses`
  - **餐厅详情页 (`RestaurantDetail.vue`)**:
    - 功能: 展示餐厅信息、菜单列表。
    - API: `GET /api/businesses/{id}`, `GET /api/foods?business={businessId}`
  - **购物车**:
    - 功能: 添加、删除、修改商品数量。通过 Pinia (`store/cart.js`) 进行全局状态管理。
    - API: `GET /api/carts`, `POST /api/carts`, `PUT /api/carts/{id}`, `DELETE /api/carts/{id}`
  - **结账流程 (`Checkout.vue`)**:
    - 功能: 多步骤流程，包括选择地址、支付方式、确认订单。
    - API: `GET /api/addresses`, `POST /api/addresses`, `POST /api/orders`
  - **个人中心 (`Profile/`)**:
    - 功能: 用户信息管理、地址管理、订单历史。
    - API: `GET /api/user`, `PUT /api/users/{id}`, `GET /api/orders/my`, `GET /api/addresses` 等。
- **开发依赖**: 依赖于核心模块 (API, Auth) 的稳定。

### 5.2. 商家仪表盘 (`/views/merchant`)

此模块为餐厅员工设计，注重操作效率和信息的清晰展示，主要用于桌面或平板设备。

- **负责人**: Developer B
- **核心页面与组件**:
  - **仪表盘 (`Dashboard.vue`)**:
    - 功能: 实时展示新进订单（通过 WebSocket），提供接单/拒单操作。
    - API: `(WebSocket)`, `POST /orders/{orderId}/accept` (假设)
  - **菜单管理 (`MenuManagement.vue`)**:
    - 功能: 对菜品分类、菜品（含规格）进行增删改查。
    - API: `GET /api/foods`, `POST /api/foods`, `PUT /api/foods/{id}`, `DELETE /api/foods/{id}`
  - **店铺管理**:
    - 功能: 编辑店铺信息。
    - API: `GET /api/businesses/my`, `PUT /api/businesses/{id}`
  - **订单历史 (`OrderHistory.vue`)**:
    - 功能: 查看、搜索历史订单。
    - API: `GET /api/orders?business={businessId}` (假设)
- **开发依赖**: 依赖于核心模块 (API, Auth) 的稳定。

### 5.3. 管理员后台 (`/views/admin`)

此模块是平台的管理中枢，功能强大，注重数据展示、批量操作和系统配置。

- **负责人**: Developer C
- **核心页面与组件**:
  - **用户管理 (`UserManagement.vue`)**:
    - 功能: 查看、搜索、管理所有顾客和商家账户。
    - API: `GET /api/users`
  - **商家管理 (`MerchantApproval.vue`)**:
    - 功能: 审核新商家入驻申请。
    - API: `GET /api/businesses?status=pending` (假设)
  - **平台数据分析**:
    - 功能: 查看平台总销售额、订单量等关键指标。
    - API: `GET /admin/platform/stats` (假设)
- **开发依赖**: 依赖于核心模块 (API, Auth) 和通用数据展示组件（如 `DataTable.vue`）。

## 6. 开发工作流与规范

1. **版本控制**: 使用 Git，遵循 GitFlow 或类似的基于功能分支的工作流。所有代码合并到 `develop` 或 `main` 分支前必须经过 Code Review。
2. **代码规范**: 严格遵循 `.eslintrc.js` 和 `.prettierrc` 文件中定义的规则。开启 IDE 的自动格式化功能。
3. **提交信息**: 遵循 Conventional Commits 规范（如 `feat: add user login page`），便于生成变更日志和版本管理。
4. **环境管理**: 使用 `.env` 文件区分开发 (`.env.development`)、生产 (`.env.production`) 等不同环境的配置（如 API 基础路径）。

## 7. 结论

本设计文档提供了一个基于 Vue 3 的模块化前端架构方案。通过明确的技术栈、清晰的项目结构和并行的模块开发计划，能够有效支持团队协作，保证项目高质量、高效率地完成。开发人员应严格遵循本文档中的规范和设计，以确保最终产品的一致性和可维护性。