# 后端业务功能改进清单

本文档列出了当前后端代码中存在的业务功能缺陷，并提供具体的代码位置和改进建议。

## 目录

- [1. 订单管理缺陷](#1-订单管理缺陷)
- [2. 积分系统问题](#2-积分系统问题)
- [3. 钱包功能不完整](#3-钱包功能不完整)
- [4. 优惠券系统缺陷](#4-优惠券系统缺陷)
- [5. 商家申请流程](#5-商家申请流程)
- [6. 评价系统问题](#6-评价系统问题)
- [7. 购物车问题](#7-购物车问题)
- [8. 权限和安全](#8-权限和安全)
- [9. 数据一致性](#9-数据一致性)
- [10. 性能优化](#10-性能优化)
- [11. 业务规则缺失](#11-业务规则缺失)
- [12. 通知系统](#12-通知系统)

---

## 1. 订单管理缺陷

### 1.1 订单取消功能缺失 ⚠️ P0

**问题位置**: `OrderController.java`

**问题描述**:

- `OrderState.java` 定义了 `CANCELED = 0` 状态
- `OrderController` 中没有实现订单取消接口
- `updateOrderStatus` 方法明确拒绝取消状态：
  ```java
  // Line 350-352
  if (order.getOrderState().equals(OrderState.CANCELED)
      || !OrderState.isValidOrderState(orderState))
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "OrderState NOT VALID");
  ```

**影响**: 用户无法取消订单，业务流程不完整

**改进建议**:

```java
@PostMapping("/{id}/cancel")
@Operation(summary = "取消订单")
public HttpResult<Order> cancelOrder(@PathVariable Long id) {
    Order order = orderService.getOrderById(id);
    if (order.getOrderState() != OrderState.PAID) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "只能取消已支付订单");
    }

    // 退还钱包余额
    if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
        walletService.refund(order.getCustomer().getId(), order.getWalletPaid());
    }

    // 解冻积分
    if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
        pointsService.rollbackPoints(order.getCustomer().getId(),
            "ORDER_" + order.getId(), "订单取消");
    }

    // 恢复优惠券
    if (order.getUsedVoucher() != null) {
        privateVoucherService.restoreVoucher(order.getUsedVoucher().getId());
    }

    order.setOrderState(OrderState.CANCELED);
    orderService.updateOrder(order);
    return HttpResult.success(order);
}
```

### 1.2 订单状态流转校验缺失 ⚠️ P0

**问题位置**: `OrderController.java:330-398`

**问题描述**:

```java
// Line 359-362: 只检查了状态是否有效，没有检查状态转换是否合法
Integer oldOrderState = newOrder.getOrderState();
newOrder.setOrderState(orderState);
EntityUtils.updateEntity(newOrder);
orderService.updateOrder(newOrder);
```

**影响**: 可能出现非法状态转换（如已完成→配送中）

**改进建议**:

```java
// OrderService.java 添加状态转换校验
public boolean isValidStateTransition(Integer from, Integer to) {
    if (from.equals(OrderState.CANCELED) || from.equals(OrderState.COMMENTED)) {
        return false; // 已取消或已评价的订单不能再改状态
    }
    if (to.equals(OrderState.CANCELED)) {
        return from.equals(OrderState.PAID); // 只能取消已支付订单
    }
    return to > from; // 状态只能递增
}
```

---

## 2. 积分系统问题

### 2.1 积分过期处理缺失 ⚠️ P1

**问题位置**: `PointsService.java`, `PointsBatch.java`

**问题描述**:

- `PointsBatch` 有 `expireTime` 字段但没有过期处理逻辑
- 没有定时任务扫描并处理过期积分

**改进建议**:

```java
// PointsService.java 添加过期处理方法
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void expirePoints() {
    List<PointsBatch> expiredBatches =
        pointsBatchRepository.findExpiredBatches(LocalDateTime.now());

    for (PointsBatch batch : expiredBatches) {
        int expiredPoints = batch.getAvailablePoints();
        if (expiredPoints > 0) {
            PointsAccount account = getOrCreateAccount(batch.getUser().getId());
            account.expirePoints(expiredPoints);
            pointsAccountRepository.save(account);

            batch.setAvailablePoints(0);
            pointsBatchRepository.save(batch);

            // 记录过期
            PointsRecord record = PointsRecord.createRecord(
                batch.getUser(), PointsRecordType.EXPIRE, expiredPoints,
                batch.getId().toString(), null, "积分过期");
            pointsRecordRepository.save(record);
        }
    }
}
```

### 2.2 积分冻结超时回滚 ⚠️ P1

**问题位置**: `PointsService.java:134-179`

**问题描述**:

- `freezePoints` 方法冻结积分后，如果订单长时间未完成，积分会一直冻结
- 没有超时自动解冻机制

**改进建议**:

```java
@Scheduled(fixedDelay = 3600000) // 每小时执行
public void rollbackTimeoutFrozenPoints() {
    LocalDateTime timeout = LocalDateTime.now().minusHours(24);
    List<PointsBatch> timeoutBatches =
        pointsBatchRepository.findFrozenBatchesBeforeTime(timeout);

    for (PointsBatch batch : timeoutBatches) {
        if (batch.getTempOrderId() != null && batch.getFrozenPoints() > 0) {
            rollbackPoints(batch.getUser().getId(), batch.getTempOrderId(), "订单超时");
        }
    }
}
```

---

## 3. 钱包功能不完整

### 3.1 充值接口缺失 ⚠️ P0

**问题位置**: `WalletController.java`, `TransactionServiceImpl.java`

**问题描述**:

- `TransactionServiceImpl` 实现了 `TOP_UP` 类型的交易处理
- `WalletController` 没有暴露充值接口给用户

**改进建议**:

```java
// WalletController.java
@PostMapping("/my/topup")
@Operation(summary = "钱包充值")
public HttpResult<TransactionVO> topup(@RequestBody BigDecimal amount) {
    User me = userService.getUserWithAuthorities().orElseThrow();
    Wallet wallet = walletRepository.findByOwnerId(me.getId())
        .orElseThrow(() -> new WalletException(WalletException.NOT_FOUND));

    TransactionVO transaction = transactionService.createTransaction(
        amount, TransactionType.TOP_UP, wallet.getId(), null);
    return HttpResult.success(transaction);
}
```

### 3.2 提现接口缺失 ⚠️ P1

**问题位置**: `WalletController.java`

**问题描述**:

- `TransactionServiceImpl.java:82-100` 实现了提现逻辑（最小金额、冷却期）
- 但 `WalletController` 没有提现接口

**改进建议**:

```java
@PostMapping("/my/withdraw")
@Operation(summary = "钱包提现")
public HttpResult<TransactionVO> withdraw(@RequestBody BigDecimal amount) {
    User me = userService.getUserWithAuthorities().orElseThrow();
    Wallet wallet = walletRepository.findByOwnerId(me.getId())
        .orElseThrow(() -> new WalletException(WalletException.NOT_FOUND));

    TransactionVO transaction = transactionService.createTransaction(
        amount, TransactionType.WITHDRAW, null, wallet.getId());
    return HttpResult.success(transaction);
}
```

### 3.3 交易记录查询缺失 ⚠️ P1

**问题位置**: `TransactionController.java`

**问题描述**:

- 用户无法查询自己的交易历史
- `TransactionRepository` 需要添加查询方法

**改进建议**:

```java
// TransactionController.java
@GetMapping("/my")
@Operation(summary = "查询我的交易记录")
public HttpResult<List<TransactionVO>> getMyTransactions(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    User me = userService.getUserWithAuthorities().orElseThrow();
    Wallet wallet = walletRepository.findByOwnerId(me.getId())
        .orElseThrow(() -> new WalletException(WalletException.NOT_FOUND));

    Page<Transaction> transactions = transactionRepository
        .findByWalletId(wallet.getId(), PageRequest.of(page, size));
    return HttpResult.success(transactions.map(TransactionVO::new).getContent());
}
```

---

## 4. 优惠券系统缺陷

### 4.1 优惠券库存管理缺失 ⚠️ P0

**问题位置**: `PrivateVoucherServiceImpl.java:claimPrivateVoucher`

**问题描述**:

- `PublicVoucher` 有 `totalQuantity` 和 `perUserLimit` 字段
- 领取时没有检查库存和用户领取次数限制

**当前代码问题**:

```java
// PrivateVoucherServiceImpl.java 缺少库存检查
public PrivateVoucherVO claimPrivateVoucher(Long publicVoucherId, User user) {
    PublicVoucher publicVoucher = publicVoucherRepository.findById(publicVoucherId)
        .orElseThrow(() -> new PublicVoucherException(PublicVoucherException.NOT_FOUND));
    // 缺少库存检查！
    // 缺少用户领取次数检查！
}
```

**改进建议**:

```java
public PrivateVoucherVO claimPrivateVoucher(Long publicVoucherId, User user) {
    PublicVoucher publicVoucher = publicVoucherRepository.findById(publicVoucherId)
        .orElseThrow(() -> new PublicVoucherException(PublicVoucherException.NOT_FOUND));

    // 检查库存
    if (publicVoucher.getTotalQuantity() != null) {
        long claimedCount = privateVoucherRepository.countByPublicVoucherId(publicVoucherId);
        if (claimedCount >= publicVoucher.getTotalQuantity()) {
            throw new PublicVoucherException("优惠券已领完");
        }
    }

    // 检查用户领取次数
    if (publicVoucher.getPerUserLimit() != null) {
        long userClaimedCount = privateVoucherRepository
            .countByPublicVoucherIdAndUserId(publicVoucherId, user.getId());
        if (userClaimedCount >= publicVoucher.getPerUserLimit()) {
            throw new PublicVoucherException("已达到领取上限");
        }
    }

    // 创建私有优惠券...
}
```

### 4.2 优惠券恢复功能缺失 ⚠️ P0

**问题位置**: `PrivateVoucherService.java`

**问题描述**:

- 订单取消时需要恢复优惠券，但没有 `restoreVoucher` 方法
- `redeemPrivateVoucher` 只能使用，不能恢复

**改进建议**:

```java
// PrivateVoucherService.java
public void restoreVoucher(Long voucherId) throws PrivateVoucherException {
    PrivateVoucher voucher = privateVoucherRepository.findById(voucherId)
        .orElseThrow(() -> new PrivateVoucherException(PrivateVoucherException.NOT_FOUND));

    if (!voucher.getUsed()) {
        throw new PrivateVoucherException("优惠券未使用，无需恢复");
    }

    voucher.setUsed(false);
    voucher.setDeleted(false);
    EntityUtils.updateEntity(voucher);
    privateVoucherRepository.save(voucher);
}
```

---

## 5. 商家申请流程

### 5.1 审批后自动授权缺失 ⚠️ P1

**问题位置**: `MerchantApplicationController.java:approveApplication`

**问题描述**:

- 商家申请审批通过后，没有自动给用户添加 `BUSINESS` 权限
- 用户需要手动添加权限才能使用商家功能

**改进建议**:

```java
// MerchantApplicationController.java
@PatchMapping("/{id}/approve")
public HttpResult<MerchantApplication> approveApplication(@PathVariable Long id) {
    MerchantApplication application = merchantApplicationService.getApplicationById(id);
    application.setApplicationState(ApplicationState.APPROVED);
    merchantApplicationService.updateApplication(application);

    // 自动授权
    User applicant = application.getApplicant();
    AuthorityUtils.addAuthority(applicant, "BUSINESS");
    userService.updateUser(applicant);

    return HttpResult.success(application);
}
```

### 5.2 驳回原因记录缺失 ⚠️ P2

**问题位置**: `BusinessApplication.java`, `MerchantApplication.java`

**问题描述**:

- 实体类没有 `rejectReason` 字段
- 驳回时无法记录原因，用户不知道为什么被拒绝

**改进建议**:

```java
// BusinessApplication.java 添加字段
@Column(length = 500)
private String rejectReason;

// Controller 添加驳回接口
@PatchMapping("/{id}/reject")
public HttpResult<BusinessApplication> rejectApplication(
    @PathVariable Long id, @RequestBody String reason) {
    BusinessApplication application = businessApplicationService.getApplicationById(id);
    application.setApplicationState(ApplicationState.REJECTED);
    application.setRejectReason(reason);
    businessApplicationService.updateApplication(application);
    return HttpResult.success(application);
}
```

---

## 6. 评价系统问题

### 6.1 重复评价检查缺失 ⚠️ P1

**问题位置**: `ReviewController.java:50-106`

**问题描述**:

```java
// Line 78-83: 没有检查订单是否已评价
if (me.equals(customer)) {
    EntityUtils.setNewEntity(review);
    review.setOrder(order);
    review.setBusiness(business);
    review.setCustomer(customer);
    reviewService.addReview(review);
```

**改进建议**:

```java
// 添加重复检查
Review existingReview = reviewService.getReviewByOrderId(orderId);
if (existingReview != null) {
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "订单已评价");
}
```

### 6.2 删除评价后积分未回收 ⚠️ P1

**问题位置**: `ReviewController.java:141-166`

**问题描述**:

- 删除评价时没有扣回已发放的积分
- 可能被恶意利用刷积分

**改进建议**:

```java
@DeleteMapping("/{reviewId}")
public HttpResult<String> deleteReview(@PathVariable Long reviewId) {
    Review review = reviewService.getReviewById(reviewId);

    // 扣回评价积分
    try {
        internalServiceClient.rollbackReviewPoints(
            review.getCustomer().getId(),
            review.getId().toString(),
            "评价已删除");
    } catch (Exception e) {
        log.error("Failed to rollback review points: {}", e.getMessage());
    }

    EntityUtils.deleteEntity(review);
    reviewService.updateReview(review);

    Order order = review.getOrder();
    order.setOrderState(OrderState.COMPLETE);
    orderService.updateOrder(order);
    return HttpResult.success("Delete review successfully.");
}
```

---

## 7. 购物车问题

### 7.1 商品下架检查缺失 ⚠️ P2

**问题位置**: `CartController.java:addCart`

**问题描述**:

- 添加购物车时没有检查商品是否已下架（deleted=true）
- 可能导致用户购买已下架商品

**改进建议**:

```java
Food food = foodService.getFoodById(foodId);
if (food == null || food.getDeleted()) {
    return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "商品不存在或已下架");
}
```

---

## 8. 权限和安全

### 8.1 并发控制缺失 ⚠️ P0

**问题位置**: `OrderController.java:247-253`, `WalletServiceImpl.java`

**问题描述**:

```java
// Line 247-253: 钱包扣款没有并发控制
if (walletPaid.compareTo(BigDecimal.ZERO) > 0 && userWallet != null) {
    if (!userWallet.decBalance(walletPaid)) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct wallet balance");
    }
    EntityUtils.updateEntity(userWallet);
    walletRepository.save(userWallet);
}
```

**影响**: 高并发下可能出现余额扣减错误

**改进建议**:

```java
// Wallet.java 添加版本号
@Version
private Long version;

// 或使用悲观锁
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.id = :id")
Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
```

### 8.2 敏感操作日志缺失 ⚠️ P1

**问题位置**: 所有涉及金额变动的操作

**改进建议**:

```java
// 创建审计日志实体
@Entity
public class AuditLog {
    @Id @GeneratedValue
    private Long id;
    private String operation; // TOPUP, WITHDRAW, ORDER_PAY
    private Long userId;
    private BigDecimal amount;
    private String ipAddress;
    private LocalDateTime createTime;
}

// 在关键操作后记录
auditLogRepository.save(new AuditLog(
    "ORDER_PAY", user.getId(), order.getOrderTotal(), request.getRemoteAddr()));
```

---

## 9. 数据一致性

### 9.1 事务回滚不完整 ⚠️ P0

**问题位置**: `OrderController.java:256-275`

**问题描述**:

```java
// Line 262-272: 积分扣减失败时回滚了钱包，但没有回滚优惠券
if (pointsUsed > 0) {
    try {
        pointsService.freezePoints(me.getId(), pointsUsed, tempOrderId);
        orderService.addOrder(order);
        pointsService.deductPoints(me.getId(), tempOrderId, order.getId().toString());
    } catch (PointsException e) {
        // 回滚钱包
        if (walletPaid.compareTo(BigDecimal.ZERO) > 0 && userWallet != null) {
            userWallet.addBalance(walletPaid);
            walletRepository.save(userWallet);
        }
        // 缺少优惠券回滚！
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct points");
    }
}
```

**改进建议**:

```java
catch (PointsException e) {
    // 回滚钱包
    if (walletPaid.compareTo(BigDecimal.ZERO) > 0 && userWallet != null) {
        userWallet.addBalance(walletPaid);
        walletRepository.save(userWallet);
    }
    // 回滚优惠券
    if (usedVoucher != null) {
        privateVoucherService.restoreVoucher(usedVoucher.getId());
    }
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct points");
}
```

### 9.2 订单创建幂等性缺失 ⚠️ P0

**问题位置**: `OrderController.java:addOrders`

**问题描述**:

- 没有幂等性保证，网络重试可能导致重复下单
- 需要添加订单号或请求ID去重

**改进建议**:

```java
@PostMapping(value = "")
public HttpResult<Order> addOrders(
    @RequestHeader(value = "X-Request-Id", required = false) String requestId,
    @RequestBody Order order) {

    // 幂等性检查
    if (requestId != null) {
        Order existingOrder = orderService.getOrderByRequestId(requestId);
        if (existingOrder != null) {
            return HttpResult.success(existingOrder);
        }
    }

    // 创建订单时保存requestId
    order.setRequestId(requestId);
    // ... 其他逻辑
}
```

---

## 10. 性能优化

### 10.1 N+1查询问题 ⚠️ P2

**问题位置**: `OrderService.java`, `ReviewService.java`

**问题描述**:

- 查询订单列表时，关联的 `Business`, `DeliveryAddress`, `Customer` 等会产生N+1查询

**改进建议**:

```java
// OrderRepository.java
@Query("SELECT o FROM Order o " +
       "LEFT JOIN FETCH o.business " +
       "LEFT JOIN FETCH o.customer " +
       "LEFT JOIN FETCH o.deliveryAddress " +
       "WHERE o.customer.id = :customerId")
List<Order> findAllByCustomerIdWithDetails(@Param("customerId") Long customerId);
```

### 10.2 分页查询缺失 ⚠️ P2

**问题位置**: `OrderController.java:311-328`, `ReviewController.java`

**问题描述**:

- 订单列表、评价列表等没有分页，数据量大时性能差

**改进建议**:

```java
@GetMapping("")
public HttpResult<Page<Order>> listOrdersByUserId(
    @RequestParam Long userId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {

    Page<Order> orders = orderService.getOrdersByCustomerId(
        userId, PageRequest.of(page, size));
    return HttpResult.success(orders);
}
```

---

## 11. 业务规则缺失

### 11.1 商品库存管理缺失 ⚠️ P1

**问题位置**: `Food.java`

**问题描述**:

- `Food` 实体没有库存字段
- 无法限制商品销售数量，可能超卖

**改进建议**:

```java
// Food.java 添加库存字段
@Column(nullable = false)
private Integer stock = 0;

// OrderController 下单时检查库存
for (Cart cart : cartList) {
    Food food = cart.getFood();
    if (food.getStock() < cart.getQuantity()) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR,
            food.getFoodName() + " 库存不足");
    }
    food.setStock(food.getStock() - cart.getQuantity());
    foodService.updateFood(food);
}
```

### 11.2 营业时间限制缺失 ⚠️ P2

**问题位置**: `Business.java`

**问题描述**:

- 商家没有营业时间字段
- 用户可以在非营业时间下单

**改进建议**:

```java
// Business.java
private LocalTime openTime;
private LocalTime closeTime;

// OrderController 下单时检查
LocalTime now = LocalTime.now();
if (now.isBefore(business.getOpenTime()) || now.isAfter(business.getCloseTime())) {
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "商家未营业");
}
```

---

## 12. 通知系统

### 12.1 订单状态变更通知缺失 ⚠️ P2

**问题位置**: `OrderController.java:updateOrderStatus`

**问题描述**:

- 订单状态改变后没有通知用户
- 用户需要主动刷新才能看到状态变化

**改进建议**:

```java
// 创建通知服务
@Service
public class NotificationService {
    public void notifyOrderStatusChange(Order order) {
        String message = switch (order.getOrderState()) {
            case OrderState.ACCEPTED -> "商家已接单";
            case OrderState.DELIVERY -> "订单配送中";
            case OrderState.COMPLETE -> "订单已完成";
            default -> null;
        };
        if (message != null) {
            // 发送通知（WebSocket/推送/短信）
            sendNotification(order.getCustomer().getId(), message);
        }
    }
}

// OrderController 中调用
orderService.updateOrder(newOrder);
notificationService.notifyOrderStatusChange(newOrder);
```

### 12.2 积分到账通知缺失 ⚠️ P2

**问题位置**: `PointsService.java:notifyOrderSuccess`, `notifyReviewSuccess`

**问题描述**:

- 积分发放后用户不知道
- 需要主动查看积分明细

**改进建议**:

```java
// PointsService.java 发放积分后通知
public Integer notifyOrderSuccess(...) {
    // ... 发放积分逻辑

    // 通知用户
    notificationService.notifyPointsEarned(userId, points, "订单完成奖励");
    return points;
}
```

---

## 优先级总结

### P0 - 严重问题（必须修复）

1. 订单取消功能缺失
2. 订单状态流转校验缺失
3. 优惠券库存管理缺失
4. 优惠券恢复功能缺失
5. 钱包充值接口缺失
6. 并发控制缺失
7. 事务回滚不完整
8. 订单创建幂等性缺失

### P1 - 重要问题（尽快修复）

1. 积分过期处理缺失
2. 积分冻结超时回滚
3. 钱包提现接口缺失
4. 交易记录查询缺失
5. 商家审批后自动授权缺失
6. 重复评价检查缺失
7. 删除评价后积分未回收
8. 敏感操作日志缺失
9. 商品库存管理缺失

### P2 - 一般问题（逐步优化）

1. 商家申请驳回原因记录
2. 商品下架检查
3. N+1查询优化
4. 分页查询实现
5. 营业时间限制
6. 通知系统

---

## 实施建议

1. **第一阶段（1-2周）**: 修复所有P0问题，确保核心业务流程完整
2. **第二阶段（2-3周）**: 修复P1问题，完善业务功能
3. **第三阶段（持续优化）**: 逐步优化P2问题，提升用户体验

## 测试建议

每个修复都应包含：

- 单元测试：验证业务逻辑正确性
- 集成测试：验证事务和并发控制
- 压力测试：验证性能和并发安全性
