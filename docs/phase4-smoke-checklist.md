# 阶段4（catalog-service）Smoke清单执行记录

## 执行批次

- 2026-03-21（写路径迁移完成后）

## 目标

1. 验证订单侧库存写路径已从本地更新迁移到 `catalog-service` 内部接口
2. 验证下单扣库存、取消回补库存在双服务场景一致

## 已完成项

1. 代码迁移
   - `OrderApplicationService` 下单改为调用 `reserveStock`
   - `OrderApplicationService` 取消改为调用 `releaseStock`
2. 接口落地
   - `POST /api/inner/catalog/stock/reserve`
   - `POST /api/inner/catalog/stock/release`
3. 测试覆盖
   - `OrderApplicationServiceTest` 增加库存预留失败/回补失败分支
   - `CatalogInternalServiceTest` 覆盖扣库存与幂等场景
   - `CatalogInnerControllerTest` 覆盖库存接口成功/失败返回

## 待执行项（双服务实操）

1. 异常补偿演练：
   - `reserveStock` 失败回滚（钱包/券/积分）
   - 取消订单时 `releaseStock` 失败处理

## 实操结果（2026-03-21）

1. 环境：
   - `catalog-service`：`8083`
   - `elm-v2.0`：`8080`
   - `CATALOG_SERVICE_URL=http://localhost:8083/elm`
2. 用例数据：
   - `businessId=1`
   - `foodId=3`
   - `quantity=2`
   - `addressId=3`
   - `requestId=phase4-smoke-1774107101`
3. 结果：
   - 下单成功：`orderId=4`
   - 取消成功：`orderStateAfterCancel=0`
   - 库存变化：
     - `stockBefore=50`
     - `stockAfterOrder=48`
     - `stockAfterCancel=50`
   - 幂等复测：
     - 相同 `X-Request-Id` 重复下单返回同一订单 `orderIdAgain=4`
