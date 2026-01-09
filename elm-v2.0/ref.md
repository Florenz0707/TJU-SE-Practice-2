## 项目积分/钱包/优惠券分析

**概述**: 基于仓库源码阅读（关注文件见引用），总结需求要点与设计建议，供验收与改进参考。

**关联代码**:

- **Schema**: [src/main/resources/schema.sql](src/main/resources/schema.sql#L1-L120)
- 积分实现: [src/main/java/cn/edu/tju/elm/service/PointsService.java](src/main/java/cn/edu/tju/elm/service/PointsService.java#L1-L420)
- 积分批次/账户: [src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java](src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java#L1-L240), [src/main/java/cn/edu/tju/elm/model/BO/PointsAccount.java](src/main/java/cn/edu/tju/elm/model/BO/PointsAccount.java#L1-L200)
- 积分内部接口: [src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java](src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java#L1-L220)
- 积分规则: [src/main/java/cn/edu/tju/elm/model/BO/PointsRule.java](src/main/java/cn/edu/tju/elm/model/BO/PointsRule.java#L1-L200)
- 钱包/交易: [src/main/java/cn/edu/tju/elm/model/BO/Wallet.java](src/main/java/cn/edu/tju/elm/model/BO/Wallet.java#L1-L220), [src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java#L1-L260)
- 选券策略: [src/main/java/cn/edu/tju/elm/utils/TOPUPPublicVoucherSelectorImpl.java](src/main/java/cn/edu/tju/elm/utils/TOPUPPublicVoucherSelectorImpl.java#L1-L80)

**验收要点**

**1. 需求部分**

- **(1) 钱包模块：鼓励充值且避免反复充值-提现薅羊毛**: 建议采取多措并举：设置提现冷却期、最小提现限额、提现手续费或分段到账（例如充值奖励需绑定X天后可提现）、对短期高频充值/提现做风控限额与人工/自动审核（KYC）；参考钱包状态与接口在 `Wallet` 与 `TransactionServiceImpl` 中的充值/提现流程实现（见关联代码）。
- **(2) 钱包模块：透支设计**: 若支持透支，建议引入信用额度模型（每用户单独额度字段）、明确透支利率/费用、可配置的授信规则（按用户等级/风控得分），并把透支操作限于原子事务与专门的账务记录（新增负余额流水）；实现上建议在 `Wallet` 增加 `creditLimit` 与 `availableCredit()`，并在转账/支付处先校验（或条件性 decBalance 可允许负数并记录负债）。
- **(3) 积分获取与使用规则**: 当前实现使用 `PointsRule` 表配置渠道类型、比例、过期天数，发放逻辑在 `notifyOrderSuccess` / `notifyReviewSuccess` 中按规则计算并写入 `PointsBatch` 与 `PointsRecord`，可满足按渠道灵活配置。使用场景通过内部接口 `/api/inner/points/trade/*` 提供冻结/扣减/回滚能力（参见 `PointsInnerController`）。
- **(4) 积分有效期规则**: 已支持：`PointsRule.expireDays` 决定发放批次的 `expire_time`（如果为 0 则不设过期），并将每次发放保存为 `PointsBatch`（包含 `expireTime`），因此支持分批过期管理（见 `PointsBatch`）。
- **(5) 其他特色需求（主动介绍）**: 项目实现了“按批次 FIFO 消耗并优先使用快过期积分”的冻结策略（`freezePoints` 中调用 `findAvailableBatchesByUserIdOrderByExpireTime`），并通过内部客户端 `InternalServiceClient` 暴露给其他服务调用，便于微服务集成。

**2. 设计部分**

- **(1) 钱包模块：充血模型体现**: 项目采用充血域模型（实体携带行为）：`Wallet` 实体实现 `addBalance`/`decBalance` 等方法，业务逻辑集中在 `WalletService` / `TransactionServiceImpl` 中，符合充血模型设计（参见 `Wallet.java` 与 `WalletServiceImpl.java`）。
- **(2) 钱包模块：保证支付/转账原子性**: 已在服务接口与实现处使用 Spring 事务注解（例如 `TransactionService` 的方法标注 `@Transactional`），但并发场景需注意：建议在关键的余额变更使用悲观锁（JPA 的 `@Lock(PESSIMISTIC_WRITE)` 或 SQL `SELECT ... FOR UPDATE`）或乐观锁（version 字段）来避免并发超额支出；并在跨服务调用（例如积分冻结 + 支付）采用分布式事务补偿或可靠消息（Saga）模式。相关代码文件：`TransactionService` / `TransactionServiceImpl`。
- **(3) 积分模块：与其他模块的关系与交互方式**: 积分作为独立服务模块，通过内部 HTTP 接口（`/api/inner/points/*`）与订单、支付等模块解耦交互；调用方使用 `InternalServiceClient`（示例见 `InternalServiceClient.java`），交互流程：订单完成->订单服务调用 `notifyOrderSuccess` 发放积分；下单结算时调用 `freezePoints`，支付成功后调用 `deductPoints`，支付失败则 `rollbackPoints`。
- **(4) 积分模块：灵活调整获取/使用规则**: 已有 `PointsRule` 表及增删改查接口，管理员可动态调整 `ratio`、`expireDays`、启用开关。建议再增加：规则优先级、条件过滤（例如时间窗口/会员等级/活动 ID）、版本化规则以及灰度发布能力。
- **(5) 积分有效期管理与优先使用快过期积分**: 系统通过 `PointsBatch.expireTime` 存储批次到期时间，并在冻结时按 `expireTime` 升序（FIFO）锁定可用积分，保证优先消耗快过期积分。为完善，可增加定时任务（cron）清理/过期下线批次并同步用户账户扣减 `total_points`，同时在用户视图提供按 `expireTime` 列出的批次提示。
- **(6) 前后端接口是否规范**: 控制器遵循 REST 路径风格（例如 `/api/points/account/my`、`/api/wallet/my`），返回统一 `HttpResult` 结构并使用 Swagger 注解，整体比较规范。但建议：明确错误码与字段契约文档、统一 DTO 请求/响应模型、避免 Controller 中重复的权限校验逻辑（可提取过滤器或注解）。关键接口见 `PointsController`、`PointsInnerController`、`WalletController`、`TransactionController`。
- **(7) 数据库设计中如何保证数据一致性**: 已使用外键、唯一约束（例如 `points_account` 的 user_id 唯一约束），但并发写入需靠事务与行级锁保证一致性。建议：
  - 在余额/积分更改处使用显式事务（已部分使用 `@Transactional`）。
  - 对热点行（用户钱包/积分账户/批次）使用悲观锁或乐观锁（version）。
  - 对跨服务流程采用幂等/幂等化 token 与 Saga/补偿事务模式。

**发现的风险与改进建议（重要）**

- TransactionServiceImpl 中 PAYMENT 流程有逻辑疑点：在 `createTransaction` 时对付款方执行 `decBalance`（冻结/扣减），但在 `finishTransaction` 中对 `transaction.getOutWallet()` 执行 `addBalance`（将金额再次加入付款方），应为加入收款方（inWallet）。建议修复并加单元测试：文件 [src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java#L1-L260)。
- 建议为资金与积分关键接口增加并发测试、集成测试与事务回退场景测试。

**结论与下一步建议**

- 当前实现已覆盖积分分批、过期、冻结/扣减/回滚及基于规则的发放；钱包采用充血模型并支持交易记录。需关注并发一致性（锁/事务）、跨服务编排（Saga）以及上述 PAYMENT 逻辑缺陷修复。可基于当前表结构与服务接口逐步完善风控/提现策略与管理后台。

-- end --
