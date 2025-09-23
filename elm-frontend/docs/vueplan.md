# 线上点餐平台前端开发计划

## 1.0 引言

### 1.1 文档目的

本开发计划旨在为“线上点餐平台”前端项目提供一个清晰、结构化和可执行的路线图。它基于已批准的需求分析、前端设计和API规范文档，将整个项目分解为具体的开发阶段、模块和任务。

本文档的核心目标是：

- **明确开发范围**：定义每个阶段和模块需要完成的具体工作。
- **支持并行开发**：通过清晰的模块划分和接口定义，使多名开发人员能够独立、高效地进行开发。
- **统一技术标准**：确保所有开发人员遵循既定的架构、代码规范和工作流程。
- **作为进度跟踪依据**：为项目管理提供一个基准，用于跟踪开发进度和资源分配。

### 1.2 项目概述

本项目旨在构建一个综合性的线上点餐平台，服务于**顾客 (Customer)**、**商家 (Merchant)** 和 **平台管理员 (Admin)** 三种核心用户角色。前端将采用 Vue 3 技术栈，通过 RESTful API 与后端服务进行交互，并利用 WebSocket 实现关键业务的实时通信。

## 2.0 开发阶段与时间线规划

为确保项目有序进行，我们将开发过程划分为四个主要阶段。以下是一个建议的时间线（以“周”为单位），可根据团队规模和实际情况进行调整。

| 阶段                         | 主要内容                               | 预计周期  | 关键产出                                                     |
| ---------------------------- | -------------------------------------- | --------- | ------------------------------------------------------------ |
| **阶段 0：基础建设**         | 项目初始化、环境配置、核心架构搭建     | 第 1 周   | - 可运行的Vue 3 + Vite项目骨架 - 统一的代码规范配置 - CI/CD基础流程 |
| **阶段 1：核心功能开发**     | API层、认证授权、全局组件与布局        | 第 1-2 周 | - 封装完善的Axios实例 - 可用的登录、注册、路由守卫功能 - 三端（顾客、商家、后台）的基础页面布局 |
| **阶段 2：应用模块并行开发** | 顾客端、商家端、管理后台的核心业务功能 | 第 3-6 周 | - 各端主要功能模块的完成 - 实现核心业务流程闭环              |
| **阶段 3：集成、测试与优化** | 功能联调、性能优化、测试与缺陷修复     | 第 7-8 周 | - 稳定、高质量的前端应用 - 完整的测试报告 - 部署准备         |

## 3.0 任务分解 (Task Breakdown)

### 3.1 阶段 0：基础建设 (负责人: Tech Lead)

| 任务ID       | 任务名称     | 描述                                                         | 预估工时 | 依赖     |
| ------------ | ------------ | ------------------------------------------------------------ | -------- | -------- |
| **SETUP-01** | 项目初始化   | 使用 Vite 创建 Vue 3 + TypeScript 项目。                     | 0.5 天   | -        |
| **SETUP-02** | 依赖集成     | 安装并配置 Vue Router, Pinia, Axios, Element Plus, Sass, ESLint, Prettier。 | 0.5 天   | SETUP-01 |
| **SETUP-03** | 目录结构搭建 | 按照 `vuedesign.md` 文档建立项目目录结构。                   | 0.5 天   | SETUP-01 |
| **SETUP-04** | 环境配置     | 配置 `.env` 文件（development, production），定义 `VITE_API_BASE_URL` 等环境变量。 | 0.5 天   | SETUP-03 |
| **SETUP-05** | 代码规范配置 | 配置 ESLint 和 Prettier 规则，并集成到 IDE 和 Git Hooks 中，确保代码提交质量。 | 1 天     | SETUP-02 |

### 3.2 阶段 1：核心功能开发 (负责人: Tech Lead / Senior Dev)

此阶段是后续所有开发的基础，必须优先完成。

| 任务ID      | 任务名称             | 描述与技术要点                                               | 关联API                             | 预估工时 | 依赖     |
| ----------- | -------------------- | ------------------------------------------------------------ | ----------------------------------- | -------- | -------- |
| **CORE-01** | **API层封装**        | 在 `utils/request.js` 中创建 Axios 实例。实现请求拦截器（自动附加JWT）和响应拦截器（统一数据结构处理、全局错误提示、401自动刷新令牌逻辑）。 | 全部API                             | 2 天     | SETUP-04 |
| **CORE-02** | **认证模块 (Store)** | 创建 `store/auth.js` (Pinia)，管理 `token`, `user` 信息。实现 `login`, `logout`, `fetchUserInfo` 等 actions。 | `POST /api/auth`, `GET /api/user`   | 1.5 天   | CORE-01  |
| **CORE-03** | **登录/注册视图**    | 创建用户登录、注册页面组件。处理表单验证、API调用和认证状态更新。 | `POST /api/auth`, `POST /api/users` | 2 天     | CORE-02  |
| **CORE-04** | **路由与守卫**       | 在 `router/` 中配置基础路由和全局前置守卫。根据 `meta` 字段（`requiresAuth`, `roles`）实现页面访问权限控制。 | -                                   | 1.5 天   | CORE-02  |
| **CORE-05** | **全局布局组件**     | 创建三端（顾客、商家、后台）的基础 `Layout` 组件，包含通用的页头、侧边栏、内容区域等。 | -                                   | 2 天     | CORE-04  |
| **CORE-06** | **WebSocket服务**    | 封装通用的 `useWebSocket` hook 或服务，用于处理连接、消息接收、心跳和重连机制。 | -                                   | 1 天     | -        |

### 3.3 阶段 2：应用模块并行开发

此阶段的任务可以分配给不同的开发人员同时进行。

#### 3.3.1 顾客应用 (负责人: Developer A)

| 任务ID      | 任务名称                    | 描述与关键组件                                               | 关联API                                                      | 预估工时 | 依赖             |
| ----------- | --------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------- | ---------------- |
| **CUST-01** | 首页-餐厅列表               | `Home.vue`: 获取并展示附近餐厅列表，支持滚动分页加载。`RestaurantCard.vue`: 单个餐厅信息展示。 | `GET /api/businesses`                                        | 2 天     | CORE-05          |
| **CUST-02** | 搜索与筛选                  | `SearchBar.vue`, `FilterPanel.vue`: 实现按餐厅名、菜系、价格等条件筛选。 | `GET /api/businesses`                                        | 1.5 天   | CUST-01          |
| **CUST-03** | 餐厅详情与菜单              | `RestaurantDetail.vue`: 展示餐厅详情和菜单。`MenuItem.vue`: 菜单项展示，处理添加商品到购物车的操作（含规格选择）。 | `GET /api/businesses/{id}`, `GET /api/foods`                 | 3 天     | CUST-01, CART-01 |
| **CUST-04** | **购物车模块 (Store & UI)** | `store/cart.js`: 管理购物车状态。`ShoppingCart.vue`: 购物车侧边栏或页面，支持增、删、改商品。 | `GET /api/carts`, `POST /api/carts`, `PUT /api/carts/{id}`, `DELETE /api/carts/{id}` | 3 天     | CORE-02          |
| **CUST-05** | 结账流程                    | `Checkout.vue`: 多步骤结账流程，包括地址选择、支付方式、订单预览和提交。 | `POST /api/orders`, `GET /api/addresses`                     | 3 天     | CUST-04, CUST-06 |
| **CUST-06** | 个人中心-基础               | `ProfileLayout.vue`, `UserProfile.vue`: 查看和编辑个人信息。 | `GET /api/user`, `PUT /api/users/{id}`                       | 2 天     | CORE-05          |
| **CUST-07** | 地址管理                    | `AddressManagement.vue`: 对收货地址进行增删改查。            | `GET /api/addresses`, `POST /api/addresses`, `PUT /api/addresses/{id}`, `DELETE /api/addresses/{id}` | 2 天     | CUST-06          |
| **CUST-08** | 订单历史与追踪              | `OrderHistory.vue`, `OrderDetail.vue`, `OrderTracker.vue`: 查看历史订单，并使用WebSocket实时追踪订单状态。 | `GET /api/orders/my`, `GET /api/orders/{id}`                 | 3 天     | CUST-05, CORE-06 |

#### 3.3.2 商家仪表盘 (负责人: Developer B)

| 任务ID       | 任务名称       | 描述与关键组件                                               | 关联API                                                      | 预估工时 | 依赖     |
| ------------ | -------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------- | -------- |
| **MERCH-01** | 商家仪表盘     | `Dashboard.vue`: 核心视图，通过WebSocket实时接收新订单并发出提醒。提供接单/拒单操作。 | WebSocket API                                                | 3 天     | CORE-06  |
| **MERCH-02** | 订单管理       | `OrderManagement.vue`: 更新订单状态（备餐中 -> 待取餐）。    | (需要后端提供更新订单状态的API)                              | 2 天     | MERCH-01 |
| **MERCH-03** | **菜单管理**   | `MenuManagement.vue`: 对店铺的菜品进行完整的增删改查(CRUD)操作。`FoodEditor.vue`: 新增/编辑菜品的表单。 | `GET /api/foods`, `POST /api/foods`, `PUT /api/foods/{id}`, `DELETE /api/foods/{id}` | 4 天     | CORE-05  |
| **MERCH-04** | 店铺信息管理   | `BusinessProfile.vue`: 查看和编辑店铺的基本信息（名称、地址、公告、图片等）。 | `GET /api/businesses/my`, `PUT /api/businesses/{id}`         | 2 天     | CORE-05  |
| **MERCH-05** | 历史订单查询   | `OrderHistory.vue`: 查看、搜索和筛选店铺的历史订单。         | `GET /api/orders` (需通过 `businessId` 筛选)                 | 2.5 天   | CORE-05  |
| **MERCH-06** | 数据分析与报告 | `Analytics.vue`: (初步) 使用图表组件展示销售额、订单量等数据。 | (需要后端提供商家分析API)                                    | 2 天     | CORE-05  |

#### 3.3.3 管理员后台 (负责人: Developer C)

| 任务ID       | 任务名称     | 描述与关键组件                                               | 关联API                                                      | 预估工时 | 依赖     |
| ------------ | ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | -------- | -------- |
| **ADMIN-01** | **用户管理** | `UserManagement.vue`: 使用可复用的数据表格组件，展示、搜索、筛选所有用户（顾客/商家）。 | `GET /api/users`, `PUT /api/users/{id}` (更新状态), `DELETE /api/users/{id}` | 3 天     | CORE-05  |
| **ADMIN-02** | **店铺管理** | `BusinessManagement.vue`: 管理平台所有店铺，包括查看详情、修改状态等。 | `GET /api/businesses`, `PUT /api/businesses/{id}`            | 3 天     | CORE-05  |
| **ADMIN-03** | 商家入驻审核 | `MerchantApproval.vue`: 待审核商家列表，管理员可进行批准或拒绝操作。 | (需要 `GET /api/businesses?status=pending` 类似的API)        | 2 天     | ADMIN-02 |
| **ADMIN-04** | 平台数据总览 | `PlatformDashboard.vue`: 展示平台级的核心指标，如总收入、订单量、活跃用户数。 | (需要平台级分析API)                                          | 2 天     | CORE-05  |

## 4.0 跨模块接口与协同约定

为保证并行开发的顺利进行，各模块开发者需遵循以下约定：

1. **统一状态管理接口**：
   - 所有需要全局访问的认证信息（如 `userId`, `role`, `token`）必须通过 `useAuthStore()` 获取。**严禁**在组件内直接操作本地存储或自行管理认证状态。
   - 购物车状态必须通过 `useCartStore()` 访问和修改。
2. **API层是唯一数据源**：
   - 所有与后端的 HTTP 通信都**必须**通过 `/api` 目录下封装好的函数进行。**严禁**在组件中直接使用 Axios 发起请求。
   - Developer A/B/C 在开发各自模块时，如果发现 `openapi.json` 中缺失必要的API，或现有API不满足需求，应立即与后端团队沟通，**不得**自行假定或模拟接口。
3. **共享组件规范**：
   - 任何可能在两个或以上模块中复用的组件（如数据表格、图片上传器、地址选择器等）都应被创建在 `/components` 目录下，并作为通用组件进行开发和维护。
   - 通用组件的 `props` 和 `emits` 必须有清晰的定义和文档注释。
4. **路由定义**：
   - 路由跳转统一使用 `name` 而不是 `path`，以降低因URL变更带来的维护成本。
   - 页面组件在 `views` 目录下按 `{端}/{模块}/Page.vue` 的结构组织。

## 5.0 开发工作流与代码质量

1. **版本控制**：
   - **分支模型**: 遵循 GitFlow。`main` 为主分支，`develop` 为开发分支。
   - **功能开发**: 从 `develop` 创建功能分支，命名为 `feat/task-id-description` (例如: `feat/CUST-05-checkout-flow`)。
   - **代码审查**: 所有功能分支合并到 `develop` 前，必须发起 Pull Request (PR)，并至少获得一名其他开发人员的审查批准 (Code Review)。
2. **代码规范**：
   - 严格遵循项目中配置的 ESLint 和 Prettier 规则。推荐在IDE中安装相关插件并开启“保存时自动格式化”。
3. **提交信息**：
   - 遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范，格式为 `<type>(<scope>): <subject>`。例如：`feat(customer): add address management page`。这有助于自动化生成变更日志。

## 6.0 测试策略

- **单元测试**: 对 `/utils` 下的工具函数和 `/store` 下的 Pinia stores 编写单元测试，确保核心逻辑的正确性。
- **组件测试**: 对 `/components` 下的通用组件进行测试，验证其 props, emits 和 slots 的行为。
- **端到端 (E2E) 测试**: 对关键用户流程（如注册登录、搜索-加购-下单、商家接单等）编写 E2E 测试脚本。

本文档为项目的启动和执行提供了全面的指导。所有团队成员应仔细阅读并严格遵守。随着项目的推进，本文档可能会进行更新和迭代。