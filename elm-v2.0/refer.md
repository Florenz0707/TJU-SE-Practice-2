# 积分 / 钱包 / 优惠券 — 验收要点与设计建议

以下为根据代码实现与改进工作的整理（便于验收与实现决策）。

## 验收要点

1. 需求部分：

- (1) 钱包模块：如何鼓励充值但又要避免利用反复充值-提现来薅羊毛？
  - 充值激励策略：用分层激励（例如首充奖励、累计消费返利、节假日活动）提高用户留存。
  - 防止套利措施：设置提现冷却期（如 24 小时）、最小提现限额、提现手续费或对奖励类金额分期解锁（例如奖励 X 天后可提现）。
  - 风控与限制：对短期高频充值/提现行为触发风控（风控阈值、临时冻结、人工审核、KYC），对异常账户降低奖励比率或禁止提现。
  - 透明规则与提示：在前端展示奖励可提现时间与规则，避免用户误解。

- (2) 钱包模块：如何设计透支功能？
  - 信用额度模型：为用户维护 `creditLimit`，根据风控/等级/历史行为分配额度。
  - 明确成本：规定透支利息或手续费、还款规则、逾期惩罚与黑名单策略。
  - 可配置化授信规则：支持按等级/风控分数自动调整额度与审批流程。
  - 会计与流水：透支应产生专门的负债流水，并与余额变更原子化写入（便于审计）。
  - 风险控制：对透支交易限制场景（如提现不可使用透支资金），并提供催收/回收机制。

- (3) 积分模块：积分获取与使用规则
  - 获取规则：使用 `PointsRule` 配置渠道（ORDER、COMMENT 等）、比例（ratio）与过期天数（expireDays），按渠道发放积分（例如金额 * ratio）。
  - 使用规则：支持冻结（下单时锁定批次积分）、扣减（支付成功后真正扣减）、回滚（支付失败或取消时解冻）。
  - 兑换与抵扣比率：明确积分与金额换算（示例代码中 100 积分 = 1 元），并将该规则对外公布。可支持白名单活动或阶梯抵扣上限。
  - 幂等与防滥用：积分发放与抵扣接口需幂等（通过 bizId / tempOrderId），防止重复发放或重复扣减。

- (4) 积分模块：积分有效期规则
  - 批次管理：每次发放生成 `PointsBatch`，记录 `expireTime`；支持无到期（expireDays=0）与有到期的批次。
  - 过期处理：定期任务清理过期批次并减少账户 `total_points`，并在用户侧展示即将过期的积分提醒。

- (5) 其他特色需求（主动介绍）
  - 优先消耗快过期积分（FIFO）：冻结/扣减逻辑按 `PointsBatch.expireTime` 升序选择批次。
  - 内部服务调用适配：提供 `InternalServiceClient`，订单/支付服务可调用 `freeze/deduct/rollback` 接口完成积分联动。
  - 选券策略插件化：`PublicVoucherSelector` 接口允许为不同场景注入选券策略（例如 TOPUP 使用 `TOPUPPublicVoucherSelectorImpl`）。

1. 设计部分：

- (1) 钱包模块：充血模型怎么体现？
  - 实体封装行为：`Wallet` 为充血模型示例，包含 `addBalance`/`decBalance`/`decBalanceWithCredit` 等方法，业务层调用实体方法而非直接操作字段，保证领域行为聚合在实体内。
  - 服务分层：`WalletService`、`TransactionService` 负责更高层事务协调（发券、写交易记录、调用外部服务）。

- (2) 钱包模块：如何保证支付转账的原子性？
  - 本地事务：关键写入（余额/交易/流水）使用数据库事务（Spring 的 `@Transactional`）。
  - 行级锁：对热点账户使用悲观锁（`SELECT ... FOR UPDATE` 或 JPA `@Lock(PESSIMISTIC_WRITE)`）或乐观锁（`version` 字段）以防并发超额支出。
  - 跨服务一致性：对涉及积分冻结/支付/订单等跨服务流程采用 Saga（可靠消息或补偿事务）或异步补偿机制，并保证接口幂等。

- (3) 积分模块：积分模块与其他模块的关系和交互方式
  - 解耦接口：积分通过内部 REST 接口（`/api/inner/points/*`）与订单/支付通信，调用方使用 `InternalServiceClient`。
  - 典型流程：订单完成 -> `notifyOrderSuccess`（发放积分）；下单结算 -> `freezePoints`；支付成功 -> `deductPoints`；支付失败 -> `rollbackPoints`。
  - 权限与安全：内部接口需鉴权（内部 token），避免被外部滥用。

- (4) 积分模块：如何灵活调整积分获取和使用规则？
  - 配置化规则表：`PointsRule` 存储 channelType、ratio、expireDays、isEnabled，支持增删改查供管理员在线调整。
  - 扩展条件：支持按用户等级、活动 ID、时间窗口等维度扩展规则字段或规则引擎接入。
  - 规则版本与灰度：支持规则版本化与灰度发布（小范围用户/活动验证后全量开启）。

- (5) 积分模块：如何实现积分有效期的管理，帮助用户优先使用快过期的积分？
  - 批次优先消费：实现按 `PointsBatch.expireTime` 升序的冻结/扣减（代码中已有 `findAvailableBatchesByUserIdOrderByExpireTime`）。
  - 定时过期处理：定期任务将到期批次标记为已过期、更新账户总额并发通知用户。
  - 用户视图友好化：在用户积分界面列出各批次到期时间与可用量，提供即将到期提醒与一键优先使用策略。

- (6) 前后端接口是否规范
  - 目前控制器遵循 REST 风格并统一返回 `HttpResult`，接口使用 Swagger 注解，整体规范。
  - 建议补充：详细契约文档（请求/响应示例）、统一错误码表（便于前端处理）、并对内部接口做更严格鉴权与限流。

- (7) 数据库设计中如何保证数据一致性
  - 约束与外键：使用外键（如 wallet.owner、points_batch.record_id）与唯一约束（如 points_account.user_id）保证基本一致性。
  - 事务与锁：在余额/积分调整处使用事务与行级锁；对高并发热点采用悲观锁或乐观锁策略并结合重试。
  - 幂等与补偿：跨服务流程采用幂等设计（使用唯一业务标识 bizId/tempOrderId）并结合补偿事务（Saga）处理失败场景。

---

## 附：本仓库已实现与改进说明（便于验收确认）

- PAYMENT 流程修复：`TransactionServiceImpl.finishTransaction` 已将 PAYMENT 的入账目标修正为收款方 `inWallet`，避免回流到付款方。
- 钱包透支与提现控制：`Wallet` 增加 `creditLimit` 与 `lastWithdrawalAt`，并实现 `decBalanceWithCredit`；提现路径添加最小提现额与冷却期检查。
- 并发安全改进：`Wallet` 的余额变更方法改为线程安全（synchronized），并在测试中添加并发调用示例；建议在真实部署时以数据库行级锁替代对象级同步以获得正确的分布式一致性。
- Schema 与测试：已在 `schema.sql` 增加 `credit_limit` 与 `last_withdrawal_at` 列，添加 Python 静态检查与 JUnit 单元测试以验证行为。

请告诉我是否需要我：

- 把 `refer.md` 同步为仓库文档（commit + push），或
- 基于上述建议继续实现库存/限领、规则引擎或集成测试示例。

### 代码位置与示例（便于验收）

- 钱包（Wallet）
  - 代码位置：`src/main/java/cn/edu/tju/elm/model/BO/Wallet.java`
  - 关键字段/方法示例：

```java
// 减余额并允许使用透支额度（示例）
public synchronized void decBalanceWithCredit(BigDecimal amount) {
    BigDecimal available = balance.add(creditLimit);
    if (available.compareTo(amount) < 0) {
        throw new InsufficientBalanceException();
    }
    balance = balance.subtract(amount);
}
```

- 交易服务（TransactionServiceImpl）
  - 代码位置：`src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java`
  - PAYMENT 完成时入账修复示例：

```java
if (TransactionType.PAYMENT.equals(tx.getType())) {
    // 正确地把钱记入收款方钱包（inWallet）
    Wallet in = walletRepository.findById(tx.getInWalletId()).orElseThrow();
    in.addBalance(tx.getAmount());
    walletRepository.save(in);
}
```

- 数据库模式（schema.sql）
  - 代码位置：`src/main/resources/schema.sql`
  - 新增列示例：

```sql
ALTER TABLE wallet
ADD COLUMN credit_limit DECIMAL(19,2) DEFAULT 0 NOT NULL,
ADD COLUMN last_withdrawal_at TIMESTAMP NULL;
```

- 积分批次（PointsBatch）与优先消费
  - 代码位置：`src/main/java/cn/edu/tju/elm/model/RECORD/PointsBatch.java`
  - 优先消费查询示例（按到期时间升序）：

```java
List<PointsBatch> batches = pointsBatchRepository
    .findAvailableBatchesByUserIdOrderByExpireTime(userId);
// 逐批扣减（FIFO）
for (PointsBatch b : batches) {
    int use = Math.min(need, b.getAvailable());
    b.decrease(use);
    need -= use;
    if (need <= 0) break;
}
```

- 内部服务客户端（积分冻结/扣减/回滚）
  - 代码位置：`src/main/java/cn/edu/tju/elm/utils/InternalServiceClient.java`
  - 调用示例：

```java
internalServiceClient.freezePoints(userId, orderId, pointsToFreeze);
// 支付成功后
internalServiceClient.deductPoints(userId, orderId);
// 支付失败时
internalServiceClient.rollbackPoints(userId, orderId);
```

- 选券策略（示例：TOPUPPublicVoucherSelectorImpl）
  - 代码位置示例：`src/main/java/cn/edu/tju/elm/service/voucher/TOPUPPublicVoucherSelectorImpl.java`
  - 选择逻辑示例片段：

```java
// 简化的选券逻辑：按门槛与价值排序，返回首个满足条件的券
Optional<Voucher> pick = vouchers.stream()
    .filter(v -> v.getMinTopUp().compareTo(amount) <= 0)
    .sorted(Comparator.comparing(Voucher::getPriority).reversed()
            .thenComparing(Voucher::getAmount).reversed())
    .findFirst();
```

以上示例为关键路径的最小可读片段；验收时请定位到对应文件并检查具体实现细节与单元测试覆盖。
