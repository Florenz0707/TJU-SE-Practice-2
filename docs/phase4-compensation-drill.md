# 阶段4补偿演练记录（2026-03-21）

## 1. 目标

验证目录服务异常时，订单侧库存相关补偿与状态一致性行为是否符合预期。

## 2. 演练场景

### 场景A：取消订单时 `releaseStock` 不可达

1. 先在 `catalog-service` 正常运行时创建订单：
   - `requestId=phase4-drill-release-fail-1774107461`
   - 订单创建成功：`orderId=5`
2. 停止 `catalog-service`（模拟库存回补接口不可达）
3. 调用取消接口：`POST /elm/api/orders/5/cancel`

结果：

1. 取消返回失败：`success=false`
2. 失败信息：`取消订单失败: 库存回补失败`
3. 订单状态保持 `PAID(1)`，未错误推进到 `CANCELED`

结论：

1. 取消链路在库存回补失败时会中断并保持订单状态一致，符合预期。

### 场景B：目录服务不可达时下单

1. 保持 `catalog-service` 停止状态
2. 发起下单：
   - `requestId=phase4-drill-catalog-down-1774107545`
3. 检查订单列表中是否存在同 `requestId` 订单

结果：

1. 下单失败：`success=false`
2. 失败信息：`Business NOT FOUND`（读取商家快照失败）
3. `requestId` 对应订单不存在：`orderExistsByRequestId=0`

结论：

1. 目录服务不可达时下单会在前置校验阶段失败，不会产生脏订单。

## 3. 备注

1. 本次演练基于最新改造：库存预留已前置到落单前，避免“先落单后扣库存”不一致风险。
2. `reserveStock`/`releaseStock` 的接口契约与联调基线见：
   - `docs/phase4-linkage-runbook.md`
   - `docs/phase4-smoke-checklist.md`
