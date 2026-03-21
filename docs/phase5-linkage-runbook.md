# 阶段5：order-service 联调 Runbook（初稿）

## 1. 目的

验证 `order-service` 在双服务模式下可独立承接订单写入与取消，并保持幂等行为稳定。

## 2. 参与服务

1. `elm-v2.0`（8080）
2. `order-service`（8084）

## 3. 前置条件

1. 数据库可访问，`order-service` 使用 `elm_order` 库
2. 两个服务均可启动并返回健康响应
3. 内部接口连通：
   - `GET /elm/api/inner/order/ping`

## 4. 执行步骤

1. 启动 `order-service`
2. 调用 `POST /elm/api/inner/order/create` 创建订单（带 `requestId`）
3. 使用相同 `requestId` 重试创建，确认返回同一订单
4. 调用 `POST /elm/api/inner/order/{orderId}/cancel` 取消订单
5. 重复取消已取消订单，确认返回状态校验失败
6. 用非所属用户取消，确认返回权限校验失败

## 5. 验收标准

1. 创建接口幂等生效：同一 `requestId` 不生成重复订单
2. 取消接口仅允许订单所属用户操作
3. 订单状态仅允许从 `PAID` 迁移到 `CANCELED`
4. 关键日志可追踪：`requestId/orderId/customerId`

## 6. 回滚策略

1. 若 `order-service` 写接口异常，切回单体本地 `OrderService + OrderDetailetService` 写路径
2. 保留 `order-service` 只读查询接口用于并行核对
3. 回滚后执行最小 smoke：下单、取消、查询订单
