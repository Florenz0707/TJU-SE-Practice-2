# 项目代码阅读生成的需求与设计说明

*说明：以下内容全部来源于代码实现（未阅读项目现有文档）。引用的源码位置以链接给出，便于复核。*

## 一、需求部分

(1) 钱包模块：如何鼓励充值但避免反复充值-提现薅羊毛？

- 设计要点：充值时触发发放“私有优惠券（PrivateVoucher）”。代码在创建 TOP_UP
  类型交易时会调用公券选择器并为用户创建私券：见 [TransactionServiceImpl](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java)。
-
公券属性：门槛（threshold）、面值（faceValue）、是否可领取（claimable）、有效天数（validDays），见 [PublicVoucher](src/main/java/cn/edu/tju/elm/model/BO/PublicVoucher.java)
与 [PublicVoucherVO](src/main/java/cn/edu/tju/elm/model/VO/PublicVoucherVO.java)。
- 私券行为：领取后记录到 `PrivateVoucher`，含到期时间（expiryDate），使用时通过 `redeem()`
  标记删除并判断是否过期，见 [PrivateVoucher](src/main/java/cn/edu/tju/elm/model/BO/PrivateVoucher.java)
  与私券服务实现 [PrivateVoucherServiceImpl](src/main/java/cn/edu/tju/elm/service/serviceImpl/PrivateVoucherServiceImpl.java)。
- 防滥用措施（代码中已有）：最低提现金额与提现冷却期（24
  小时），以及记录最后提现时间：见 [TransactionServiceImpl](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java)
  中的 `MIN_WITHDRAWAL`、`WITHDRAWAL_COOLDOWN_SECONDS` 与 `lastWithdrawalAt` 字段。
- 结论：通过“充值发券 + 提现门槛与冷却”来鼓励充值同时抑制频繁充值提现套利。

(2) 钱包模块：如何设计透支功能？

- 代码实现：`Wallet` 有 `creditLimit` 字段，提供 `decBalanceWithCredit(BigDecimal)` 方法，允许扣减使余额变为负但不超过
  `creditLimit`，见 [Wallet](src/main/java/cn/edu/tju/elm/model/BO/Wallet.java)。
- 结论：采用每个钱包可配置的信用额度，在扣款时检查 `balance + creditLimit >= amount`，满足则允许透支。

(3) 积分模块：积分获取和使用规则

- 获取：由可配置的 `PointsRule` 控制（按渠道 channelType），`notifyOrderSuccess` / `notifyReviewSuccess` 按规则计算并发放积分（创建
  `PointsRecord` 与 `PointsBatch`），见 [PointsService](src/main/java/cn/edu/tju/elm/service/PointsService.java)
  与 [PointsRule](src/main/java/cn/edu/tju/elm/model/BO/PointsRule.java)。
- 使用：积分抵扣采取“先锁定（freeze）- 支付成功后扣除（deduct）/ 回滚（rollback）”的流程。锁定按可用批次（按过期时间优先）分配，使用临时订单号
  `tempOrderId`
  关联锁定批次，见 [PointsService.freezePoints]/[PointsInnerController](src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java)
  与批次模型 [PointsBatch](src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java)。
- 积分与货币换算：代码示例中前端返回示例 `moneySaved = points / 100`（即默认 100 积分 = 1
  元，非强制规则，仅用于返回说明），见 [PointsService.freezePoints](src/main/java/cn/edu/tju/elm/service/PointsService.java)。

(4) 积分模块：积分有效期规则

- 规则存储：`PointsRule.expireDays`；发放时若 `expireDays>0` 则创建 `PointsBatch` 时设置 `expireTime = now + expireDays`
  ，见 [PointsService.notifyOrderSuccess](src/main/java/cn/edu/tju/elm/service/PointsService.java)
  与 [PointsBatch](src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java)。
- 锁定/使用选择策略：按 `expireTime` 升序（`NULL` 放后）选择可用批次（实现了按过期时间优先
  FIFO），见 [PointsBatchRepository.findAvailableBatchesByUserIdOrderByExpireTime](src/main/java/cn/edu/tju/elm/repository/PointsBatchRepository.java)。

(5) 其他特色需求（代码中主动体现）

- 公券选择器：`TOPUPPublicVoucherSelectorImpl`
  为充值场景提供“最佳券选择”策略（可扩展不同场景选择器），见 [TOPUPPublicVoucherSelectorImpl](src/main/java/cn/edu/tju/elm/utils/TOPUPPublicVoucherSelectorImpl.java)。
- 交易模型支持“异步完成”：`Transaction` 有 `isFinished` 标记，`createTransaction` 仅做初步变更（如对付款方冻结或提现立即修改），
  `finishTransaction` 将把 PAYMENT
  的金额入账给收款方，见 [Transaction](src/main/java/cn/edu/tju/elm/model/BO/Transaction.java)
  与 [TransactionServiceImpl](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java)。
- 内部接口模式：积分对外通过内部 REST 接口（`/api/inner/points/...`
  ）对订单/评价系统集成，见 [PointsInnerController](src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java)
  与内部调用客户端 `InternalServiceClient`。

## （二）设计部分

(1) 钱包模块：充血模型体现

- 体现位置：`Wallet` 实体里包含余额、代金券、信用额度、以及余额/代金券的增删（`addBalance`/`decBalance`/`addVoucher`/
  `decVoucher`/`decBalanceWithCredit`
  ）等业务方法，表明域对象自身承担业务逻辑（充血模型），见 [Wallet](src/main/java/cn/edu/tju/elm/model/BO/Wallet.java)。

(2) 钱包模块：如何保证支付转账的原子性？

- 当前实现状况：`TransactionServiceImpl` 在一个业务方法里按步骤修改多张表（wallet save、transaction save 等），但类上没有标注
  `@Transactional`
  （见 [TransactionServiceImpl](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java)
  ），因此事务边界在当前代码里不是明确的单一数据库事务。实体内部对余额操作使用 `synchronized(this)`（`Wallet.addBalance`/
  `decBalance`），这只在单实例 JVM 中有效，无法保证分布式或多进程并发安全，且无法替代数据库事务。
- 建议（从代码可行改进点）：
    - 为重要方法加 `@Transactional`（例如 `createTransaction`、`finishTransaction`）保证单个数据库事务原子性；
    - 为并发安全：推荐在 DB 层使用乐观锁（给 `BaseEntity` 增加 `@Version` 字段）或在关键更新时使用悲观锁（
      `SELECT ... FOR UPDATE` / JPA 的 `@Lock(PESSIMISTIC_WRITE)`）；
    - 避免仅依赖 `synchronized`，因为无法跨进程/多个实例生效。

(3) 积分模块：积分模块与其他模块的关系和交互方式

- 积分为独立服务（`PointsService`），对外暴露两类接口：用户自助查询/查看接口（`/api/points`
  ，见 [PointsController](src/main/java/cn/edu/tju/elm/controller/PointsController.java)）以及内部系统调用接口（
  `/api/inner/points`
  ，见 [PointsInnerController](src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java)）。
- 典型交互：订单系统在订单完成时调用积分的 `notify/order-success`；在支付确认时调用 `freeze`/`deduct`/`rollback`
  三步完成积分抵扣流程，见 [InternalServiceClient](src/main/java/cn/edu/tju/elm/utils/InternalServiceClient.java)
  与 [PointsInnerController](src/main/java/cn/edu/tju/elm/controller/PointsInnerController.java)。

(4) 积分模块：如何灵活调整积分获取和使用规则？

- 代码支持点：`PointsRule` 为数据库可配置实体（channelType、ratio、expireDays、isEnabled），且有管理员接口
  `PointsAdminController` 提供增删改查，见 [PointsRule](src/main/java/cn/edu/tju/elm/model/BO/PointsRule.java)
  与 [PointsAdminController](src/main/java/cn/edu/tju/elm/controller/PointsAdminController.java)。
- 建议：若要更灵活，可引入规则优先级/版本号或支持多条规则叠加策略（目前实现取
  `findByChannelTypeAndIsEnabled(...).getFirst()`），并在 `PointsService` 中支持策略插件化。

(5) 积分模块：如何实现有效期管理并优先使用快过期积分？

- 代码实现：发放时创建 `PointsBatch`（带 expireTime）；锁定/使用时通过仓库方法
  `findAvailableBatchesByUserIdOrderByExpireTime` 按 `expireTime ASC`（将 NULL
  放到最后）取得可用批次，从而实现“快过期优先”使用；见 [PointsBatch](src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java)
  与 [PointsBatchRepository](src/main/java/cn/edu/tju/elm/repository/PointsBatchRepository.java)。
- 建议补充：增加定时任务清理过期批次并把过期积分从账户中扣减（代码中
  `PrivateVoucherServiceImpl.clearExpiredPrivateVouchers` 为空，类似的清理逻辑应实现于积分模块）。

(6) 前后端接口是否规范

- 优点：整体采用 REST 风格，统一返回结构 `HttpResult`，路径语义清晰（如 `/api/wallet`, `/api/transaction`, `/api/points`
  ），并在内部 DTO/VO 层封装输入输出。
- 问题/不规范点：
    - 在 `WalletController.addVoucher` 中使用了两个 `@RequestBody` 参数（
      `@RequestBody Long walletId, @RequestBody BigDecimal amount`），Spring MVC 不允许在同一方法上有多个 `@RequestBody`
      ，这是一个明显的错误，需要改为单一 DTO（或 path/param + body
      的组合），见 [WalletController](src/main/java/cn/edu/tju/elm/controller/WalletController.java)。
    - `PrivateVoucherService.createPrivateVoucher` 接口上标注了 `@Transactional`
      （接口层），而实现类没有在类上统一标注，可能会受到代理类型的影响（接口式代理能工作，但在复杂配置下建议在实现类或方法上显式标注）。

(7) 数据库设计中如何保证数据一致性

- 已有保障：JPA 仓库与实体映射，`PointsService` 使用 `@Transactional`
  （类级别），并把积分相关的批次/账户更新包装在事务里以保证一致性，见 [PointsService](src/main/java/cn/edu/tju/elm/service/PointsService.java)。
- 改进建议：
    - 为跨表（wallet、transaction、voucher）更新加事务（为 `TransactionServiceImpl` 加 `@Transactional`）；
    - 对高并发的余额更新使用 DB 级锁或乐观锁（增加 `@Version` 字段实现乐观锁）或在查询时使用悲观锁；
    - 对积分批次的冻结/扣减在事务内完成（当前实现已在 `PointsService` 中），并通过唯一索引/约束防止重复扣减；
    - 对于分布式场景，建议引入幂等/去重机制（通过 tempOrderId/finalOrderId）和分布式事务或可靠消息补偿方案。

附：关键源码参考（便于复核）

- 钱包模型：[src/main/java/cn/edu/tju/elm/model/BO/Wallet.java](src/main/java/cn/edu/tju/elm/model/BO/Wallet.java)
- 交易与发券逻辑：[src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java)
- 公券/私券：[src/main/java/cn/edu/tju/elm/model/BO/PublicVoucher.java](src/main/java/cn/edu/tju/elm/model/BO/PublicVoucher.java)
- 私券服务：[src/main/java/cn/edu/tju/elm/service/serviceImpl/PrivateVoucherServiceImpl.java](src/main/java/cn/edu/tju/elm/service/serviceImpl/PrivateVoucherServiceImpl.java)
- 积分核心：[src/main/java/cn/edu/tju/elm/service/PointsService.java](src/main/java/cn/edu/tju/elm/service/PointsService.java)
- 积分批次与仓库：[src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java](src/main/java/cn/edu/tju/elm/model/BO/PointsBatch.java)
- 前端接口示例：钱包/交易/积分控制器分别见 [WalletController](src/main/java/cn/edu/tju/elm/controller/WalletController.java)
