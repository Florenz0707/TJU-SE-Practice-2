# 阶段5：order-service 联调 Runbook（购物车+评价迁移后）

## 1. 目的

验证 `order-service` 在四服务模式下可承接订单/地址/购物车/评价主链路，并保持幂等与补偿行为稳定。

## 2. 参与服务

1. `elm-v2.0`（8080）
2. `points-service`（8081）
3. `account-service`（8082）
4. `catalog-service`（8083）
5. `order-service`（8084）

## 3. 前置条件

1. 数据库可访问，`order-service` 使用 `elm_order` 库
2. 所有服务均可启动并返回健康响应
3. 内部接口连通：
   - `GET /elm/api/inner/order/ping`
4. 脚本环境准备完成：
   - `cd elm-v2.0/scripts`
   - `uv sync`
   - `.env` 已按 `integration.env.example` 配置

## 4. 执行步骤

1. 执行脚本化联调：
   - `cd elm-v2.0/scripts && uv run run_four_service_smoke.py --env-file .env`
2. 校验订单链路：
   - 下单成功、取消成功、完成态更新成功
3. 校验购物车链路：
   - 下单前购物车读取成功
   - 下单后购物车清理成功（无遗留脏数据）
4. 校验评价链路：
   - 评价新增后订单状态从 `COMPLETE` 迁移到 `COMMENTED`
   - 删评后订单状态回滚到 `COMPLETE`
5. 校验 outbox：
   - `POINTS_ORDER_SUCCESS` 事件状态应为 `SENT`

## 5. 验收标准

1. 创建接口幂等生效：同一 `requestId` 不生成重复订单
2. 取消接口仅允许订单所属用户操作
3. 订单状态流转符合约束（含 `PAID->CANCELED`、`COMPLETE->COMMENTED`、删评回滚）
4. 购物车与地址远程调用稳定，无本地仓储兜底依赖
5. `integration_outbox_event` 中 `POINTS_ORDER_SUCCESS` 为 `SENT`

## 6. 回滚策略

1. 若 `order-service` 写接口异常，先切只读兜底与编排降级开关，暂停评价/删评入口
2. 保留 `order-service` 只读查询接口用于并行核对
3. 回滚后执行最小 smoke：下单、取消、查询订单、购物车读取、地址读取
4. 回滚完成后保留异常样本 `requestId/orderId/reviewId`，用于补偿复盘

## 7. 执行记录（2026-03-22，基础版）

1. 执行环境：
   - `order-service`：`8084`
   - 请求基地址：`http://localhost:8084/elm/api/inner/order`
2. 用例结果：
   - `GET /ping`：成功
   - `POST /create`：成功，生成 `orderId=1`
   - 同 `requestId` 重试 `POST /create`：返回同 `orderId=1`（幂等通过）
   - `POST /{orderId}/cancel`（owner）：成功
   - 重复取消：失败，消息 `OrderState NOT PAID`
   - 非 owner 取消：失败，消息 `AUTHORITY LACKED`
3. 结论：
   - 阶段5基础 smoke 通过，可继续推进订单读链路迁移与四服务联调

## 8. 四服务联调记录（2026-03-22）

1. 参与服务：
   - `points-service`（8081）
   - `account-service`（8082）
   - `catalog-service`（8083）
   - `order-service`（8084）
   - `elm-v2.0`（8080，编排入口）
2. 执行摘要：
   - 新建 smoke 用户并完成钱包/地址/购物车准备
   - 下单（钱包支付）成功：`ORDER1_OK=true`
   - 取消成功：`CANCEL1_OK=true`
   - 第二单下单成功：`ORDER2_OK=true`
   - 更新订单到完成态成功：`COMPLETE_OK=true`
3. 联调核验：
   - `account/catalog/order` 取消链路成功闭环
   - `integration_outbox_event` 新增 `POINTS_ORDER_SUCCESS` 且状态 `SENT`，确认 points 通道联通

## 9. 四服务联调复验（购物车+评价迁移后，2026-03-22）

1. 执行命令：
   - `cd elm-v2.0/scripts && uv run run_four_service_smoke.py --env-file .env`
2. 样例输出：
   - `SMOKE_OK=true`
   - `ORDER1_ID=10`
   - `ORDER2_ID=11`
   - `CANCEL1_STATE=0`
   - `COMPLETE2_STATE=4`
   - `OUTBOX_EVENT_TYPE=POINTS_ORDER_SUCCESS`
   - `OUTBOX_STATUS=SENT`
3. 结论：
   - 阶段5迁移链路可继续推进边界态回归与回滚脚本核对
