# 阶段5业务对账记录（2026-03-22）

## 1. 目标

在当前无生产实例灰度切换需求的前提下，完成本地业务一致性对账，验证订单域拆分后关键账务链路闭环：

1. 订单扣款交易一致
2. 取消订单退款交易一致
3. 完成订单 Outbox 事件投递一致

## 2. 对账范围

以最近 5 笔订单为样本（`elm_order.orders`）进行跨库核对：

1. `elm_order.orders`
2. `elm_account.transaction`
3. `elm.integration_outbox_event`（`POINTS_ORDER_SUCCESS`）

## 3. 对账规则

1. 扣款一致性：
   - `transaction.biz_id = orders.request_id`
   - `transaction.amount = orders.wallet_paid`
2. 退款一致性：
   - 若 `orders.order_state = CANCELED(0)`，存在 `transaction.biz_id = ORDER_{orderId}` 且金额一致
   - 若非取消态，不应存在 `ORDER_{orderId}` 退款交易
3. Outbox 一致性：
   - 若 `orders.order_state = COMPLETE(4)`，`integration_outbox_event` 中应存在 `POINTS_ORDER_SUCCESS` 且 `status=SENT`
   - 事件 payload 的 `bizId` 对应 `orderId`

## 4. 执行方式

在 `elm-v2.0/scripts` 下执行 `uv run python` 对账查询（基于 `.env` 读取本地数据库连接配置）。

## 5. 本次结果

1. 样本订单：`11, 10, 9, 8, 7`
2. 明细判定：
   - `order_id=11`：`debit_ok=true`, `refund_ok=true`, `outbox_ok=true`
   - `order_id=10`：`debit_ok=true`, `refund_ok=true`, `outbox_ok=true`
   - `order_id=9`：`debit_ok=true`, `refund_ok=true`, `outbox_ok=true`
   - `order_id=8`：`debit_ok=true`, `refund_ok=true`, `outbox_ok=true`
   - `order_id=7`：`debit_ok=true`, `refund_ok=true`, `outbox_ok=true`
3. 汇总结果：`RECON_OK=true`

## 6. 结论

阶段5当前样本下账务链路一致性通过。可继续推进：

1. 更多角色/异常组合边界回归
2. `account-service` 收口中的业务链路级灰度回滚实操
