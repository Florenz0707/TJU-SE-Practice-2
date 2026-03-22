# 阶段3（account-service）Smoke清单执行记录

## 执行批次

- 2026-03-21（双服务联调）

## 目标

1. 验证订单侧资金与券能力已从本地调用迁移到 `account-service` 内部接口
2. 验证下单与取消链路在双服务场景下账券一致

## 已执行项

1. 服务启动检查
   - `account-service`（8082）启动成功
   - `elm-v2.0`（8080）并配置 `ACCOUNT_SERVICE_URL=http://localhost:8082/elm`
2. 钱包链路
   - 用户创建钱包并充值成功
   - 下单后钱包扣款成功
   - 取消后钱包退款成功
3. 优惠券链路
   - 管理员创建公共券成功
   - 用户领取私有券成功
   - 下单后私有券核销（`deleted=true`）
   - 取消后私有券回滚（`deleted=false`）
4. 交易审计链路
   - `GET /api/inner/account/transaction/by-biz/{requestId}` 可查到下单扣款交易
   - `GET /api/inner/account/transaction/by-biz/ORDER_{orderId}` 可查到取消退款交易
5. 订单状态链路
   - 下单成功创建订单
   - 取消后状态为 `CANCELED(0)`

## 样例数据

1. `requestId`: `smoke-order-1774103637`
2. `orderId`: `3`
3. 关键结果：
   - `ORDER_STATE_AFTER_CANCEL=0`
   - `TX_EXISTS=true`
   - `VOUCHER_DELETED_BEFORE_CANCEL=true`
   - `VOUCHER_DELETED_AFTER_CANCEL=false`

## 待补充项

1. 异常补偿演练（已补齐脚本与记录，2026-03-22）：
   - 脚本：`elm-v2.0/scripts/run_phase3_account_drill.py`
   - 记录：`docs/phase3-compensation-drill.md`
   - 覆盖：钱包扣款/退款幂等、券回滚失败分支、不可达探测
   - 最近结果：`DRILL_OK=true`
2. 灰度开关与回滚策略演练（待业务链路级实操）：
   - 配置级开关脚本已落地：
     - `elm-v2.0/scripts/manage_account_gray.py`
     - `elm-v2.0/scripts/rollback_account_gray.py`
   - 脚本可执行验证通过（`--skip-verify`）
   - 业务链路级实操（2026-03-22）：
     - `8082` 基线 smoke：通过（`SMOKE_OK=true`）
     - 切换异常目标 `8099` 后回滚到 `8082`：smoke 通过（`SMOKE_OK=true`）
     - 回滚到 `8080`：下单失败（`Failed to load wallet`）
   - 结论：当前阶段回滚目标应使用上一个可用地址（`ACCOUNT_SERVICE_URL_PREVIOUS`），不建议固定写 `8080`
