# 阶段5：order-service 拆分准备（2026-03-21）

## 1. 目标

将订单域（订单/订单明细/购物车/地址/评价）从单体中独立为 `elm-microservice/order-service`，并通过内部接口与 `points-service`、`account-service`、`catalog-service` 协同。

## 2. 范围

1. 订单主链路：
   - 创建订单
   - 取消订单
   - 订单状态流转
2. 订单从属域：
   - 订单明细
   - 购物车
   - 配送地址
   - 评价（含评价相关积分通知）

## 3. 现状（拆分前）

1. 编排入口：
   - `OrderApplicationService`
2. 跨服务依赖已改造：
   - 积分：`InternalServiceClient`
   - 账户：`InternalAccountClient`
   - 目录：`InternalCatalogClient`
3. 已具备能力：
   - 订单库存扣减/回补远程调用
   - 钱包/券远程调用
   - 积分冻结/扣减/返还远程调用

## 4. 拆分边界建议

1. `order-service` 内部维护：
   - `Order`、`OrderDetailet`、`Cart`、`DeliveryAddress`、`Delivery`、`Comment`
2. 外部域只通过内部 API：
   - 用户信息由 token claim + 兼容层填充
   - 不直接访问 account/catalog/points 表
3. 对外 API 路径保持兼容：
   - 仍以 `/elm/api/orders|carts|addresses|comments/**` 暴露

## 5. 迁移优先级

1. 第一步：迁移只读查询接口（订单/购物车/地址）
2. 第二步：迁移写接口（下单/取消/状态流转）
3. 第三步：迁移评价链路与积分通知
4. 第四步：网关切流与单体退役对应模块

## 6. 风险点与控制

1. 幂等键统一：
   - 下单 `X-Request-Id`
   - 账户/库存回滚 requestId 规则保持一致
2. 事务边界：
   - 本地事务仅覆盖订单域数据
   - 跨服务失败由补偿策略兜底
3. 兼容性：
   - 返回 DTO 结构保持前端兼容（继续走兼容填充）

## 7. 下一步执行项

1. 将单体下单/取消链路中的订单本地落库调用迁移到 `order-service` 写接口
2. 接入 account/catalog/points 内部调用客户端并完成端到端回归
3. 输出并执行阶段5联调 runbook 初稿

## 8. 已交付（2026-03-21）

1. `elm-microservice/order-service` 工程骨架已创建
2. 最小模型迁移完成（去外部实体耦合）：
   - `Order`（`businessId/addressId/voucherId`）
   - `OrderDetailet`（`orderId/foodId/quantity`）
3. 仓储与内部查询服务已落地：
   - `OrderRepository`
   - `OrderDetailetRepository`
   - `OrderInternalService`
4. 内部接口第一版：
   - `GET /api/inner/order/ping`
   - `GET /api/inner/order/{orderId}`
   - `GET /api/inner/order/by-request/{requestId}`
   - `GET /api/inner/order/customer/{customerId}`
   - `GET /api/inner/order/{orderId}/details`
5. 单元测试通过：
   - `OrderInternalServiceTest`
   - `OrderInnerControllerTest`
6. 写接口与事务编排第一版已落地：
   - `POST /api/inner/order/create`（`requestId` 幂等）
   - `POST /api/inner/order/{orderId}/cancel`（仅订单所属用户）
7. 测试环境兼容补充：
   - `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
   - 使用 `mock-maker-subclass` 规避 inline attach 限制
8. 单体本地调用迁移（写路径）已完成：
   - 新增 `InternalOrderClient`
   - 下单幂等查询/创建调用切到 `order-service`
   - 取消订单查询明细与状态更新调用切到 `order-service`
9. 单体迁移回归测试通过：
   - `mvn -f elm-v2.0/pom.xml -Dtest=OrderApplicationServiceTest test`
10. 订单读链路迁移进行中（已完成第一批）：
    - `OrderController` 查询接口改走 `OrderApplicationService -> InternalOrderClient`
    - 新增 `order-service` 商家维度查询接口：`GET /api/inner/order/business/{businessId}`
    - `FoodController` 按订单查菜品改为读取 `order-service` 明细后回查 `food-service`
    - `ReviewController` 订单存在性校验改为读取 `order-service`
11. 订单状态流转链路迁移完成：
    - `order-service` 新增状态更新内部接口：`POST /api/inner/order/{orderId}/state`
    - 单体 `OrderApplicationService.updateOrderStatus` 改为远程状态更新
    - `ReviewApplicationService` 评价/删评触发的订单状态变更改为远程更新
12. 四服务联调 smoke 已执行（2026-03-22）：
    - 下单（钱包）成功、取消成功、完成态更新成功
    - Outbox `POINTS_ORDER_SUCCESS` 状态为 `SENT`
