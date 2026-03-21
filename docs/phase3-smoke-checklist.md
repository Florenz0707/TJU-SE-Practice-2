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

1. 异常补偿演练：
   - 下单中途 `account-service` 不可达
   - 取消时券回滚失败
2. 灰度开关与回滚策略演练：
   - 配置级开关切回单体本地能力或降级路径
3. 固化自动化脚本：
   - 把本次手工 smoke 命令整理为可重复脚本
