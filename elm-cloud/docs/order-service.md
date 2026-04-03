# order-service 微服务详情

## 1. 功能概述
`order-service` 负责统筹外卖平台中的核心业务交易和订单流程。主要职责为：
- 接收客户端发送的点餐数据并创建订单及关联的订单菜品明细 (`OrderDetailet`)。
- 查询当前用户/商户的订单列表，支持分页与快照查询。
- 实现付款、取消等状态的流转管理。

## 2. 数据库与表结构
- 数据库连接：使用独立或共享的交易类数据库（如 `elm_order`）。
- 核心实体与表：
  - `Order` (在老单体中名为 `Orders`)：包含 `id`, `customer_id` (用户), `business_id` (商家), `order_total` (总价), `delivery_address_id` (地址), `order_state` (订单状态)。
  - `OrderDetailet`：记录一笔订单下的多种菜品项目及数量（`food_id` 和 `quantity`）。

## 3. 提供的接口

### 3.1 外部暴露接口 (外部通过 Gateway 调用)
所有新的外部调用统一前缀位于 `/elm/api/orders`:
- **GET /api/orders/user/my**：通过 `Authorization/JwtUtils` 识别当前登录用户，拉取历史订单快照。
- **GET /api/orders/user/my/page**：附加 `page` 与 `size` 获取当前用户的分页订单流。
- **GET /api/orders/business/{id}**：获取指派于选定商家的订单数据供商家后台处理端展示。
- **POST /api/orders**：将客户端的请求、配送地址、订单项目转为实体，生成交易金额并状态标记为代付款(未支付)。
- **POST /api/orders/{id}/cancel**：用户可以手动取消未处于流转末期的订单。

- **兼容的单体端点 (`/OrdersController/**`)**：
  保持了与单体 Vue 客户端提交的 `application/x-www-form-urlencoded` 数据格式兼容，如：
  - `/OrdersController/createOrders`
  - `/OrdersController/getOrdersById`
  - `/OrdersController/listOrdersByUserId`

### 3.2 内部接口
以 `/api/inner/order/` 提供跨微服务的调用（如商家确认、退款补偿等）：
- **GET /api/inner/order/{orderId}**：获取指定ID订单的基础数据。
- **POST /api/inner/order/create**：接受详细的后端指令强连贯创建。

## 4. 迁移细节与遇到的问题
- **剥离深度联表与 `CreateOrderCommand` 实体化适配**：老单体采用极端的 Hibernate 表级联和复杂模型。在拆分至新微服务之后，采用了 CQRS 近似理念（`CreateOrderCommand` Record 持有创建请求参数并以扁平方式处理）。但遇到了旧版表单映射与 Java 16+ `record` 不太兼容的情况，需要专门添加 `LegacyOrder` 包装对象来专门接受老前端对 userId 甚至是 String 类型的兼容绑定，再内部手工映射成最新的 Command 以调用服务层的安全流水接口。
- **JwtUtils 的注入支持**：我们在该 Controller 里使用了复用的 `JwtUtils` 服务进行 Token 拆解而免于被绑定在一套特定网关规范中，这提高了该鉴权解析层面的鲁棒性，并在失败或未传令牌时进行平滑 fallback 到 1L。