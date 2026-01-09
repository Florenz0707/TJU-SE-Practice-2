# 优惠券系统 设计与实现说明

本文档按“数据结构、关键代码、调用顺序、与其他业务关联、注意点与改进建议”结构，说明当前仓库中优惠券（Voucher）模块的实现细节。

## 主要源码位置

- 管理（公共券）Controller: [PublicVoucherController.java](src/main/java/cn/edu/tju/elm/controller/PublicVoucherController.java#L1-L200)
- 管理服务实现: [PublicVoucherServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/PublicVoucherServiceImpl.java#L1-L200)
- 公共券实体: [PublicVoucher.java](src/main/java/cn/edu/tju/elm/model/BO/PublicVoucher.java#L1-L200)
- 公共券 VO: [PublicVoucherVO.java](src/main/java/cn/edu/tju/elm/model/VO/PublicVoucherVO.java#L1-L200)
- 公共券仓库: [PublicVoucherRepository.java](src/main/java/cn/edu/tju/elm/repository/PublicVoucherRepository.java#L1-L120)
- 选择器接口/实现: [PublicVoucherSelector.java](src/main/java/cn/edu/tju/elm/utils/PublicVoucherSelector.java#L1-L50)、[TOPUPPublicVoucherSelectorImpl.java](src/main/java/cn/edu/tju/elm/utils/TOPUPPublicVoucherSelectorImpl.java#L1-L120)
- 私有/用户券实体: [PrivateVoucher.java](src/main/java/cn/edu/tju/elm/model/BO/PrivateVoucher.java#L1-L200)
- 私有券 VO & 仓库 & 服务实现: [PrivateVoucherVO.java](src/main/java/cn/edu/tju/elm/model/VO/PrivateVoucherVO.java#L1-L200)、[PrivateVoucherRepository.java](src/main/java/cn/edu/tju/elm/repository/PrivateVoucherRepository.java#L1-L120)、[PrivateVoucherServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/PrivateVoucherServiceImpl.java#L1-L200)
- 私有券 Controller（新增）: [PrivateVoucherController.java](src/main/java/cn/edu/tju/elm/controller/PrivateVoucherController.java#L1-L200)
- 交易触发点: [TransactionServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java#L1-L200)
- 钱包相关: [WalletController.java](src/main/java/cn/edu/tju/elm/controller/WalletController.java#L1-L200)、[WalletService.java](src/main/java/cn/edu/tju/elm/service/serviceInterface/WalletService.java#L1-L200)

## 数据结构（实体与字段要点）

- `PublicVoucher`（公共券）:
  - 字段：`threshold`（门槛金额）、`faceValue`（面值）、`claimable`（是否可被领取）、`validDays`（有效期天数）、软删除继承 `BaseEntity` 字段。
  - 管理接口允许管理员增删改查公共券。
- `PrivateVoucher`（用户券 / 私有券）:
  - 字段：`wallet`（所属钱包）、`faceValue`、`expiryDate`、软删除标记等。
  - 方法：`redeem()` 会把券标记删除并返回是否在有效期内。
  - 通过 `PrivateVoucherRepository` 持久化，并新增了按 wallet.owner 查询方法 `findByWalletOwnerId`。
- `Wallet` / `WalletVO`:
  - 钱包用于承载私有券的归属，提供创建钱包与增值接口，私有券与钱包关联以支持按用户查询。

## 关键代码与调用顺序（主流程）

1) 管理员创建公共券（Admin）
   - 管理员通过 `PublicVoucherController` 的 `POST /api/publicVoucher` 创建券，调用 `PublicVoucherServiceImpl.createPublicVoucher`，写入 `public_voucher` 表。

2) 用户触发场景（示例：TOP_UP）
   - `TransactionServiceImpl.createTransaction` 在 `TOP_UP` 场景下构造 `TransactionVO`，并调用 `publicVoucherService.chooseBestPublicVoucherForTransaction(...)`，选择器使用 `TOPUPPublicVoucherSelectorImpl`。
   - `PublicVoucherServiceImpl.chooseBestPublicVoucherForTransaction` 已改为在有金额时调用 `PublicVoucherRepository.findQualifiedPublicVoucher(amount)` 以预筛选并按 `faceValue desc` 排序，然后将结果转换为 `PublicVoucherVO` 传入选择器。
   - 若选择到某个 `PublicVoucherVO`，会通过 `PrivateVoucherService.createPrivateVoucher(walletId, publicVoucherVO)` 向用户发放 `PrivateVoucher`（私有券）。

3) 用户领取/列出/核销流程（新增接口）
   - 领取（claim）：`POST /api/privateVoucher/claim/{publicVoucherId}`（`PrivateVoucherController`），需要登录。流程：
     - 验证登录用户，从 `WalletService.getWalletByOwner` 获取用户钱包；
     - 读取 `PublicVoucher` 转为 VO 后调用 `PrivateVoucherService.createPrivateVoucher`（创建私有券并保存）。
   - 列出用户券：`GET /api/privateVoucher/my` 返回 `PrivateVoucherVO` 列表（基于 `PrivateVoucherRepository.findByWalletOwnerId(userId)`）。
   - 核销（redeem）：`POST /api/privateVoucher/redeem/{id}` 调用 `PrivateVoucherService.redeemPrivateVoucher(id)`，内部调用 `PrivateVoucher.redeem()`（标记删除并判断是否在有效期内），保存并返回结果。

4) 选择器逻辑
   - `TOPUPPublicVoucherSelectorImpl` 实现：筛选 `claimable==true`、满足 `threshold <= amount` 的券，并选择 `faceValue` 最大的券（已修正阈值比较与空金额保护）。


## 与其他业务的关联点

- 交易系统（Transaction）：在 `TOP_UP` 情况下会触发公共券的选择与私有券发放（`TransactionServiceImpl.createTransaction`）。
- 钱包（Wallet）：`PrivateVoucher` 关联 `Wallet`，钱包由用户创建与持有；领取私有券时会把券关联到用户的钱包。
- 鉴权：所有用户动作接口均使用 `UserService.getUserWithAuthorities()` 检查登录状态并依赖 JWT 认证（`/api/auth`）。管理员接口使用 `@PreAuthorize("hasAuthority('ADMIN')")`。

## 注意点与边界条件

- 软删除：实体继承 `BaseEntity`，使用 `EntityUtils` 的软删除/替换策略，查询方法需过滤 `deleted` 字段（服务层已有过滤保护）。
- 过期处理：`PrivateVoucher.expiryDate` 存在，但当前没有批处理任务定期清理或生成过期流水，需在后续增加定期任务。
- 库存与限领：当前模型无库存或每用户限领机制（若需活动控制或防刷，需要扩展 `PublicVoucher` 字段如 `totalQuantity`、`perUserLimit`，并在领取逻辑中做原子检查/更新）。
- 并发安全：领取与核销在高并发场景下需要加锁或使用乐观锁/数据库约束以避免超发或重复核销。

## 补充（最近变更与建议）

- 近期改动（代码/Schema/测试）说明：
  - 已修复交易入账逻辑：`TransactionServiceImpl.finishTransaction` 在 PAYMENT 场景中应将金额入账给收款方（`inWallet`），已修正（参见 `TransactionServiceImpl.java`）。
  - 支持钱包透支与提现控制：`Wallet` 添加了 `creditLimit` 与 `lastWithdrawalAt` 字段，并实现 `decBalanceWithCredit()`；`TransactionServiceImpl` 在提现路径加入了最小提现金额与提现冷却期校验（参见 `Wallet.java` 与 `TransactionServiceImpl.java`）。
  - 更新了数据库模式：在 `schema.sql` 中新增 `wallet` 表列 `credit_limit` 与 `last_withdrawal_at`（参见 `src/main/resources/schema.sql`）。
  - 添加了静态单元测试（Python）：`tests/test_wallet_changes.py`，用于静态验证 schema 与 Java 源码中新增字段/方法与常量是否存在，便于 CI 做快速检查。

- 对优惠券系统的进一步建议（优先级高到低）：
  1. 库存与限领支持：为 `PublicVoucher` 增加 `totalQuantity`、`perUserLimit` 字段，并在领取逻辑使用数据库原子更新（行锁或 UPDATE ... WHERE remaining>0 返回行数）以避免超发。
  2. 领取幂等与防刷：在 `PrivateVoucher` 表记录 `claim_trace_id` 或使用唯一索引（user_id + public_voucher_id + biz_id），并在高频领取点使用令牌桶/速率限制。
  3. 定期任务：实现定时任务（Quartz / Spring Scheduled）清理过期私有券并产生过期统计/通知。
  4. 领取并发控制：Controller->Service 中对关键路径引入乐观锁（version）或悲观锁（SELECT FOR UPDATE），并在失败时重试短次。
  5. API 合约与错误码：补充 API 文档，明确错误码（例如 4001: 优惠券已过期，4002: 不满足门槛，4003: 超出领取限制），便于前端处理。
  6. 集成测试：为关键流程（TOP_UP->发券、领取、核销）编写端到端集成测试，覆盖并发场景与事务回滚。

- 推荐的短示例 API（已实现/建议）:
  - 领取公共券（基于已实现的私有券创建）
    - POST /api/privateVoucher/claim/{publicVoucherId}
    - 入参：无（从 Token 获取用户），返回：201 + `PrivateVoucherVO` 或 4xx 错误码与消息
  - 列出我的券
    - GET /api/privateVoucher/my
    - 返回：200 + `List<PrivateVoucherVO>`
  - 核销券
    - POST /api/privateVoucher/redeem/{id}
    - 返回：200 + 布尔成功标志或具体失败原因

总结：本次补充将项目中与优惠券强相关的改动（钱包透支、提现控制、交易入账修复、schema 更新）整理到文档中，同时给出若干高价值改进点与短期实现建议（库存/限领、幂等、过期任务、并发控制、错误码与测试）。建议按优先级先实现库存与限领、领取幂等与 API 错误码规范，再补充集成测试与定时过期清理。
