# 阶段4：catalog-service 拆分准备（2026-03-21）

## 1. 目标

将商家/菜品/申请审核能力从单体中抽离到 `elm-microservice/catalog-service`，并为订单侧提供稳定的库存与商家信息内部接口。

## 2. 当前代码盘点

## 2.1 控制器

- `BusinessController`
- `FoodController`
- `BusinessApplicationController`
- `MerchantApplicationController`

## 2.2 服务

- `BusinessService`
- `FoodService`
- `BusinessApplicationService`
- `MerchantApplicationService`

## 2.3 仓储与模型

- Repository: `BusinessRepository`, `FoodRepository`, `BusinessApplicationRepository`, `MerchantApplicationRepository`
- Model: `Business`, `Food`, `BusinessApplication`, `MerchantApplication`

## 3. 订单侧耦合点（优先解耦）

当前 `OrderApplicationService` 直接依赖：

1. `BusinessService.getBusinessById`（商家存在性、营业时间、起送价、配送费）
2. `Cart` 挂载的 `Food` 实体本地读取（价格、库存）
3. 本地库存扣减/回补：
   - 下单时 `food.decreaseStock(...)` + `foodService.updateFood(...)`
   - 取消时 `food.increaseStock(...)` + `foodService.updateFood(...)`

拆分目标：订单侧改为通过 `InternalCatalogClient` 调用 `catalog-service`，不再直接读写商家/菜品表。

## 4. 内部接口草案（第一版）

## 4.1 商家查询

1. `GET /api/inner/catalog/business/{businessId}`
   - 返回：营业时间、起送价、配送费、商家状态

## 4.2 菜品与库存

1. `GET /api/inner/catalog/food/{foodId}`
   - 返回：价格、库存、上架状态、businessId
2. `POST /api/inner/catalog/stock/reserve`（建议）
   - 入参：`requestId`,`orderId`,`userId`,`items[{foodId,quantity}]`
   - 语义：批量扣减库存，幂等
3. `POST /api/inner/catalog/stock/release`（建议）
   - 入参：`requestId`,`orderId`,`reason`
   - 语义：批量回补库存，幂等

## 5. 一致性与幂等约束

1. 库存扣减/回补统一使用 `requestId/orderId` 做幂等键
2. 订单侧避免“本地事务 + 远程强依赖”导致不一致
3. 库存接口建议支持按 `orderId` 查询最近操作，便于补偿判定

## 6. 下一步执行计划

1. 迁移 `Business/Food` 最小可运行集（controller/service/repository/model）
2. 实现 `GET /api/inner/catalog/business/{id}`、`GET /api/inner/catalog/food/{id}` 真实查询逻辑
3. 再实现库存扣减/回补内部接口与单测
4. 最后改造 `OrderApplicationService` 接入 `InternalCatalogClient`

## 7. 已交付（2026-03-21）

1. `elm-microservice/catalog-service` 工程骨架已创建
2. 基础启动配置已就位（`application.properties`，端口 `8083`）
3. 内部接口已落地真实查询（第一版）：
   - `GET /api/inner/catalog/business/{businessId}`
   - `GET /api/inner/catalog/food/{foodId}`
4. `Business/Food` 最小模型、Repository、Service 已迁移到 `catalog-service`
5. 单元测试已补充并通过：
   - `CatalogInternalServiceTest`
   - `CatalogInnerControllerTest`
6. 编译与测试验证通过：
   - `mvn -f elm-microservice/catalog-service/pom.xml -DskipTests compile`
   - `mvn -f elm-microservice/catalog-service/pom.xml test`
7. 订单侧读取链路迁移完成（第一批）：
   - 新增 `InternalCatalogClient`
   - `OrderApplicationService` 已改为远程读取商家与菜品快照进行下单校验

## 8. 当前进行中（2026-03-21）

1. 库存写接口第一版已实现（`reserve/release`，按 `requestId` 幂等）
2. 单体 `InternalCatalogClient` 已补齐库存调用方法并接入订单侧
3. `OrderApplicationService` 已切换库存写路径到远程（reserve/release）
4. 双服务 smoke 已执行通过（下单扣库存、取消回补、下单幂等）
5. 下一步补齐异常补偿演练记录（reserve/release 失败场景）
