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
2. `pom.xml`、`application.properties`、`README.md` 已就位
3. 可编译验证通过（见下文）

## 7. 最近执行记录

1. 双服务联调（阶段2）已打通：
   - `elm-v2.0` + `points-service` 同时运行
   - Outbox 事件 `POINTS_ORDER_SUCCESS`、`POINTS_REVIEW_SUCCESS` 均转为 `SENT`
2. 阶段3准备已启动：
   - 完成域边界盘点
   - 输出内部接口草案
