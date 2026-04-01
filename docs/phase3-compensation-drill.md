# 阶段3：account-service 异常补偿演练记录（2026-03-22）

## 1. 目标

1. 验证钱包扣款/退款内部接口幂等性（重复 `requestId` 不产生重复交易）
2. 验证券回滚失败场景可被业务识别（返回 `data=false`，不抛系统级异常）
3. 验证不可达场景可被快速探测（连接错误可复现并定位）

## 2. 执行方式

1. 脚本：`elm-v2.0/scripts/run_phase3_account_drill.py`
2. 命令：
   - `cd elm-v2.0/scripts`
   - `uv run run_phase3_account_drill.py --env-file .env`
3. 配置来源：
   - `integration.env.example`（本地复制为 `.env`）
   - 关键参数：`PHASE3_DRILL_USER_ID`、`PHASE3_DRILL_AMOUNT`

## 3. 演练项与结果

1. 钱包扣款幂等（重复 `requestId`）
   - 预期：两次调用返回同一 `transactionId`
   - 结果：通过（`debit_idempotent=true`）
2. 钱包退款幂等（重复 `requestId`）
   - 预期：两次调用返回同一 `transactionId`
   - 结果：通过（`refund_idempotent=true`）
3. 券回滚失败分支（非法 `voucherId`）
   - 预期：接口可用，返回 `data=false`
   - 结果：通过（`rollback_failed_as_expected=true`）
4. account-service 不可达探测
   - 预期：连接错误可稳定复现
   - 结果：通过（`unreachable_simulation_ok=true`）

## 4. 样例输出字段

1. `debit_request_id`
2. `refund_request_id`
3. `debit_idempotent`
4. `refund_idempotent`
5. `rollback_failed_as_expected`
6. `unreachable_simulation_ok`
7. `DRILL_OK`

## 5. 最近一次执行结果（2026-03-22）

1. 执行命令：
   - `cd elm-v2.0/scripts && uv run run_phase3_account_drill.py --env-file .env`
2. 样例结果：
   - `user_id=1`
   - `amount=3.00`
   - `bootstrap_refund_tx_id=13`
   - `debit_request_id=phase3-debit-84ee98dd`
   - `refund_request_id=phase3-refund-84ee98dd`
   - `debit_idempotent=True`
   - `refund_idempotent=True`
   - `rollback_failed_as_expected=True`
   - `unreachable_simulation_ok=True`
   - `DRILL_OK=True`

## 6. 结论与后续

1. 阶段3自动化演练脚本已固化，可重复执行并输出标准结果字段
2. 下一步建议补充“业务链路级不可达演练”：
   - 在四服务联调中注入 `ACCOUNT_SERVICE_URL` 不可达，验证下单失败与无脏订单残留
