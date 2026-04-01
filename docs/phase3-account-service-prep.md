# 阶段3：account-service 拆分准备（2026-03-21）

## 1. 目标

将钱包/交易/券能力从单体中抽离到 `elm-microservice/account-service`，并提供稳定的内部幂等接口给订单侧调用。

## 2. 当前代码盘点

### 2.1 控制器

- `WalletController`
- `TransactionController`
- `PublicVoucherController`
- `PrivateVoucherController`

### 2.2 服务

- `WalletServiceImpl`
- `TransactionServiceImpl`
- `PublicVoucherServiceImpl`
- `PrivateVoucherServiceImpl`

### 2.3 仓储与模型

- Repository: `WalletRepository`, `TransactionRepository`, `PublicVoucherRepository`, `PrivateVoucherRepository`
- Model: `Wallet`, `Transaction`, `PublicVoucher`, `PrivateVoucher`
- VO/Record: `WalletVO`, `TransactionVO`, `PublicVoucherVO`, `PrivateVoucherVO`, `TransactionsRecord`

## 3. 订单侧耦合点（需优先解耦）

当前 `OrderApplicationService` 直接依赖：

1. `WalletRepository` / `WalletService`
2. `PrivateVoucherRepository` / `PrivateVoucherService`

拆分目标：改为通过 `InternalAccountClient` 调用 `account-service` 内部接口，不再直连钱包/券仓储。

## 4. 内部接口草案（第一版）

### 4.1 钱包

1. `POST /api/inner/account/wallet/debit`
   - 入参：`requestId`,`userId`,`amount`,`bizId`,`reason`
   - 语义：扣款，幂等
2. `POST /api/inner/account/wallet/refund`
   - 入参：`requestId`,`userId`,`amount`,`bizId`,`reason`
   - 语义：退款，幂等

### 4.2 优惠券

1. `POST /api/inner/account/voucher/redeem`
   - 入参：`requestId`,`userId`,`voucherId`,`orderId`
   - 语义：核销，幂等
2. `POST /api/inner/account/voucher/rollback`
   - 入参：`requestId`,`userId`,`voucherId`,`orderId`,`reason`
   - 语义：回滚已核销券，幂等

### 4.3 交易审计

1. `GET /api/inner/account/transaction/by-biz/{bizId}`
   - 语义：按业务单号查询交易结果，支持补偿判定

## 5. 幂等与一致性约束

1. `requestId` 作为跨服务幂等键（钱包扣款/退款、券核销/回滚统一）
2. 单体订单侧不再执行“本地事务 + 远程强依赖”
3. 与积分一致，订单侧对资金与券也采用可重试策略（必要时接入 Outbox）

## 6. 已交付准备项

1. `elm-microservice/account-service` 工程骨架已创建（端口 `8082`）
2. 钱包/交易/券域代码已迁移（controller/service/repository/model/vo/exception/constant）
3. 内部接口已实现：`/api/inner/account/**`
4. 单元测试已补齐并通过（`AccountInnerControllerTest`、`AccountInternalServiceTest`）
5. 可编译与测试验证通过（见下文）

## 7. 最近执行记录

1. 双服务联调（阶段2）已打通：
   - `elm-v2.0` + `points-service` 同时运行
   - Outbox 事件 `POINTS_ORDER_SUCCESS`、`POINTS_REVIEW_SUCCESS` 均转为 `SENT`
2. 阶段3准备已启动：
   - 完成域边界盘点
   - 输出内部接口草案
3. 订单侧本地调用迁移已完成（2026-03-21）：
   - 新增 `InternalAccountClient`（`account.service.url` 可配置）
   - `OrderApplicationService` 已改为通过 `account-service` 内部接口处理券校验/核销回滚、钱包扣款退款
   - 新增单测 `OrderApplicationServiceTest` 覆盖远程钱包预检与取消回滚调用
4. 双服务联调 smoke 已执行（2026-03-21）：
   - 启动：
     - `account-service`（8082，`DB_URL=.../elm_account`，`DB_USERNAME=user`，`DB_PASSWORD=pass@WORD`）
     - `elm-v2.0`（8080，`ACCOUNT_SERVICE_URL=http://localhost:8082/elm`）
   - 链路：
     - 用户创建钱包并充值
     - 管理员创建公共券，用户领取私有券
     - 用户下单（带 `X-Request-Id`）使用钱包+优惠券
     - 用户取消订单
   - 验证结果：
     - 订单取消后状态为 `CANCELED(0)`
     - `GET /api/inner/account/transaction/by-biz/{requestId}` 可查到下单扣款交易
     - `GET /api/inner/account/transaction/by-biz/ORDER_{orderId}` 可查到取消退款交易
     - `GET /api/inner/account/voucher/{voucherId}` 在下单后 `deleted=true`，取消后恢复为 `deleted=false`
