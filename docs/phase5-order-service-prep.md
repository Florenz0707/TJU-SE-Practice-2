# 阶段5：order-service 拆分准备（2026-03-21）

## 1. 目标

将订单域（订单/订单明细/购物车/地址/评价）从单体中独立为 `elm-microservice/order-service`，并通过内部接口与 `points-service`、`account-service`、`catalog-service` 协同。

## 2. 范围

1. 订单主链路：
   - 创建订单
   - 取消订单
   - 订单状态流转
2. 订单从属域：
   - 订单明细
   - 购物车
   - 配送地址
   - 评价（含评价相关积分通知）

## 3. 现状（拆分前）

1. 编排入口：
   - `OrderApplicationService`
2. 跨服务依赖已改造：
   - 积分：`InternalServiceClient`
   - 账户：`InternalAccountClient`
   - 目录：`InternalCatalogClient`
3. 已具备能力：
   - 订单库存扣减/回补远程调用
   - 钱包/券远程调用
   - 积分冻结/扣减/返还远程调用

## 4. 拆分边界建议

1. `order-service` 内部维护：
   - `Order`、`OrderDetailet`、`Cart`、`DeliveryAddress`、`Delivery`、`Comment`
2. 外部域只通过内部 API：
   - 用户信息由 token claim + 兼容层填充
   - 不直接访问 account/catalog/points 表
3. 对外 API 路径保持兼容：
   - 仍以 `/elm/api/orders|carts|addresses|comments/**` 暴露

## 5. 迁移优先级

1. 第一步：迁移只读查询接口（订单/购物车/地址）
2. 第二步：迁移写接口（下单/取消/状态流转）
3. 第三步：迁移评价链路与积分通知
4. 第四步：网关切流与单体退役对应模块

## 6. 风险点与控制

1. 幂等键统一：
   - 下单 `X-Request-Id`
   - 账户/库存回滚 requestId 规则保持一致
2. 事务边界：
   - 本地事务仅覆盖订单域数据
   - 跨服务失败由补偿策略兜底
3. 兼容性：
   - 返回 DTO 结构保持前端兼容（继续走兼容填充）

## 7. 下一步执行项

1. 创建 `elm-microservice/order-service` 工程骨架
2. 先迁移 `Order` + `OrderDetailet` 最小可运行集与查询接口
3. 补齐 `order-service` 内部健康检查与基础单测
4. 输出阶段5联调 runbook 初稿
