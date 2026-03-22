# 微服务拆分执行计划（elm-v2.0）

## 1. 目标与范围

在保证现有前端接口路径尽量不变（`/elm/api/**`）的前提下，将当前单体后端拆分为以下 5 个核心服务：

1. `iam-service`：用户、认证、权限（`core` 包）
2. `points-service`：积分账户/批次/记录/规则
3. `account-service`：钱包、交易、公共券、私有券
4. `catalog-service`：商家、菜品、商家申请、开店申请
5. `order-service`：订单、订单明细、购物车、地址、评价

当前已完成的拆分前基础（沿用）：

1. 积分域已从 `User` 实体关联改为 `userId`
2. 订单完成/评价完成积分发放已改为 Outbox 异步投递
3. 订单与评价编排已下沉到应用服务（便于后续跨服务编排）

## 1.1 代码组织约定（最新）

1. 单体工程保留在 `elm-v2.0/`
2. 拆分后的微服务统一放在 `elm-microservice/` 目录下
3. 当前已落地微服务：`elm-microservice/points-service`

## 1.2 任务进展（截至 2026-03-21）

### 阶段 0（基线）

1. OpenAPI 合同文件已落地（`elm-v2.0/interfaces.openapi.json`）
2. JWT `uid` claim 与内部 token 过滤已在单体实现
3. 日志已包含 `traceId/requestId/userId/orderId` 等关键字段

状态：**已完成（满足当前拆分前提）**

### 阶段 1（模型去耦）

1. 积分域已完成 `userId` 去耦，不再强依赖 `User` 实体关联
2. 订单/评价编排已下沉应用服务，积分发放走 Outbox
3. DTO 兼容层与主链路 smoke 已完成一轮验证

状态：**已完成（持续回归中）**

### 阶段 2（points-service 拆分）

已完成：

1. 单体端 `InternalServiceClient` 调用路径补全（含 refund/review-deleted）
2. 单体端内部积分控制器补齐 `/api/inner/points/trade/refund`、`/api/inner/points/notify/review-deleted`
3. 独立 `points-service` 工程已创建并可编译运行
4. 积分域核心代码（controller/service/repository/entity/vo）已迁移到 `elm-microservice/points-service`
5. Outbox 可观测/恢复管理接口已落地（summary、dispatch-now、requeue）
6. 双服务联调完成（2026-03-21）：
   - `elm-v2.0`（8080）与 `points-service`（8081）同时运行
   - Outbox 插入 `POINTS_ORDER_SUCCESS` 与 `POINTS_REVIEW_SUCCESS` 事件后均自动转为 `SENT`

待完成：

1. 完整业务链路 smoke（下单/评价/取消从业务 API 端到端）
2. Outbox 失败恢复演练（结合新管理接口）
3. 规则与接口文档补齐（阶段交付物收口）

状态：**联调已打通，进入验收收口**

### 阶段 3（account-service 拆分）

已完成：

1. `elm-microservice/account-service` 工程骨架与核心域代码迁移完成
2. 内部接口已覆盖扣款/退款、券核销/回滚、交易查询
3. 新增订单侧预校验接口：
   - `GET /api/inner/account/wallet/by-user/{userId}?createIfAbsent=...`
   - `GET /api/inner/account/voucher/{voucherId}`
4. 单体 `OrderApplicationService` 本地钱包/券调用已迁移为 `InternalAccountClient` 远程内部调用
5. 新增与更新单元测试并通过（`account-service` 与 `elm-v2.0`）
6. 阶段3联调文档已补齐：
   - `docs/phase3-linkage-runbook.md`
   - `docs/phase3-smoke-checklist.md`

待完成：

1. 账户域独立 schema 与配置模板收口
2. 拆分后灰度开关与回滚脚本补齐
3. 异常补偿演练（account-service 不可达/回滚失败）实操记录

状态：**本地调用迁移 + 双服务 smoke 已完成，进入阶段3收口**

### 阶段 4（catalog-service 拆分准备）

已完成：

1. 输出阶段4拆分准备文档：`docs/phase4-catalog-service-prep.md`
2. 梳理订单侧与目录域耦合点：
   - 商家校验（营业时间/起送价/配送费）
   - 菜品价格与库存读取
   - 下单扣库存与取消回补库存
3. 给出 `catalog-service` 内部接口草案（商家查询、菜品查询、库存扣减/回补）
4. 创建 `elm-microservice/catalog-service` 工程骨架并编译通过
5. 完成 `catalog-service` 第一批真实实现：
   - 迁移 `Business/Food` 最小模型、Repository、Service
   - `GET /api/inner/catalog/business/{businessId}`、`GET /api/inner/catalog/food/{foodId}` 已可查询
   - 单元测试通过（`CatalogInternalServiceTest`、`CatalogInnerControllerTest`）
6. 单体订单读路径已接入 `InternalCatalogClient`：
   - 商家存在性/营业时间/起送价/配送费改为远程读取
   - 菜品价格与库存校验改为远程读取
7. `catalog-service` 库存内部接口第一版已实现（幂等键 `requestId`）：
   - `POST /api/inner/catalog/stock/reserve`
   - `POST /api/inner/catalog/stock/release`
8. `OrderApplicationService` 库存写路径已迁移到远程调用：
   - 下单走 `reserveStock`
   - 取消订单走 `releaseStock`
   - 不再本地更新 `Food.stock`
9. 阶段4联调文档已补齐：
   - `docs/phase4-linkage-runbook.md`
   - `docs/phase4-smoke-checklist.md`
10. 阶段4异常补偿演练已完成：

- `docs/phase4-compensation-drill.md`
- 覆盖 `releaseStock` 不可达与目录服务不可达下单场景

待完成：

1. 固化库存接口契约与补偿流程文档（requestId/orderId）
2. 推进阶段3剩余收口项（账户域异常补偿与灰度回滚）
3. 启动阶段5（order-service）拆分准备与边界梳理

状态：**已完成（双服务联调 + 补偿演练通过）**

### 阶段 5（order-service 拆分准备）

已完成：

1. 输出阶段5拆分准备文档：`docs/phase5-order-service-prep.md`
2. 明确拆分边界：
   - 订单/订单明细/购物车/地址/评价归属 `order-service`
   - 与 account/catalog/points 继续通过内部 API 协同
3. 创建 `elm-microservice/order-service` 工程骨架并落地健康接口：
   - `GET /api/inner/order/ping`
4. `Order + OrderDetailet` 最小可运行集已迁移到 `order-service`（去外部实体耦合）
5. `order-service` 内部查询接口第一版已落地并通过单测
6. `order-service` 写接口第一版已落地：
   - `POST /api/inner/order/create`（按 `requestId` 幂等）
   - `POST /api/inner/order/{orderId}/cancel`（仅订单所属用户）
7. `order-service` 创建/取消事务服务已实现并补齐单测覆盖
8. `order-service` Mockito 测试配置已适配当前环境（`mock-maker-subclass`）
9. 单体 `OrderApplicationService` 本地订单写调用已迁移：
   - 幂等查询改为 `InternalOrderClient.getOrderByRequestId`
   - 创建订单改为 `InternalOrderClient.createOrder`
   - 取消订单状态更新改为 `InternalOrderClient.cancelOrder`
   - 取消库存回补明细改为 `InternalOrderClient.getOrderDetailsByOrderId`
10. 新增 `InternalOrderClient` 与配置项：
    - `order.service.url=${ORDER_SERVICE_URL:http://localhost:8080/elm}`
11. 单体订单应用服务迁移单测已通过（`OrderApplicationServiceTest`）
12. 阶段5基础 smoke 已执行通过（2026-03-22）：
    - 创建订单幂等
    - 取消订单状态校验
    - 取消订单权限校验
13. 订单读链路迁移第一批已落地：
    - `OrderController` 查询接口改为 `order-service` 读取
    - `FoodController` 按订单查菜品改为 `order-service` 明细读取
    - `ReviewController` 订单存在性校验改为 `order-service` 读取
    - `order-service` 新增 `GET /api/inner/order/business/{businessId}`
14. 订单状态写链路继续迁移：
    - `order-service` 新增 `POST /api/inner/order/{orderId}/state`
    - 单体 `updateOrderStatus` 与评价链路状态变更已切到 `order-service`
15. 四服务联调（points/account/catalog/order）已执行通过（2026-03-22）：
    - 下单（钱包）成功、取消成功、完成态更新成功
    - Outbox `POINTS_ORDER_SUCCESS` 事件状态 `SENT`
16. 四服务联调命令已固化为脚本（2026-03-22）：
    - 目录：`elm-v2.0/scripts/`
    - 入口：`run_four_service_smoke.py`
    - 运行方式：`uv sync && uv run run_four_service_smoke.py`
    - 配置模板：`integration.env.example`（敏感信息通过 `.env` 本地注入）
17. 订单分页读链路迁移已完成（2026-03-22）：
    - `order-service` 新增分页内部接口：
      - `GET /api/inner/order/customer/{customerId}/page?page=&size=`
      - `GET /api/inner/order/business/{businessId}/page?page=&size=`
    - `elm-v2.0` 已接入分页查询客户端与应用服务封装
    - 对外新增分页查询接口：
      - `GET /api/orders/user/my/page?page=&size=`
      - `GET /api/orders/business/{id}/page?page=&size=`

待完成：

1. 补齐 `order-service` 状态流转与评价链路专项测试
2. 持续补齐分页与边界态回归用例（含多角色权限）

状态：**阶段5准备已启动**

## 1.3 最近计划（未来 3-5 天）

1. 推进阶段5订单域迁移收口：
   - 完成订单剩余分页/聚合查询迁移到 `order-service`
   - 补齐状态流转与评价链路专项测试
2. 执行四服务脚本化 smoke 常态回归：
   - 使用 `elm-v2.0/scripts/run_four_service_smoke.py`
   - 保持 `.env` 配置化，禁止明文提交敏感项
3. 完善阶段5联调与回滚 runbook（加入脚本化执行路径）
4. 持续执行统一质量门禁：按 `.pre-commit-config.yaml` 做风格与基础校验

---

## 2. 总体节奏（6 周）

### 阶段 0：拆分前基线（2026-03-20 ~ 2026-03-24）

**目标**：冻结接口契约、打通网关骨架、准备统一身份上下文。

**任务**：

1. 固化外部 API 契约（按 Controller 出 OpenAPI 文档）
2. 网关路由骨架落地（反向代理到单体，保持现有路径）
3. JWT 增加 `uid` claim（业务服务从 token 直接取用户ID）
4. 补充基础观测：服务日志字段统一（traceId/requestId/userId）

**验收标准**：

1. 前端功能无感知变更
2. 单体仍可完整运行
3. 新增契约文档可支撑并行拆分

---

### 阶段 1：模型去耦（2026-03-25 ~ 2026-04-01）

**目标**：消除非 IAM 域对 `User` 实体的强耦合。

**任务**：

1. 将订单/钱包/商家/地址/购物车/评价等域模型中的 `User` 关联逐步替换为 `userId`
2. 业务层改为使用“当前用户上下文（token claim）”而非直接查 `User` 实体
3. 清理跨域 Repository 直接访问

**验收标准**：

1. 编译通过
2. 核心链路 smoke：注册/登录、下单、支付、评价、取消
3. 无新的跨域实体直接依赖

---

### 阶段 2：拆分 `points-service`（2026-04-02 ~ 2026-04-08）

**目标**：以积分域为首个独立服务试点，验证服务间调用与异步补偿。

**任务**：

1. 迁移积分控制器、服务、仓储、实体到独立工程
2. 迁移内部接口：`/api/inner/points/**`
3. 保留并启用 Outbox 调度，`order-service` 仅通过内部 API + token 调用积分服务

**验收标准**：

1. 订单完成发积分成功
2. 评价发积分成功
3. 订单取消积分返还/解冻成功
4. Outbox 重试可见且可恢复

---

### 阶段 3：拆分 `account-service`（2026-04-09 ~ 2026-04-16）

**目标**：将钱包/交易/券从订单域中剥离。

**任务**：

1. 迁移 `Wallet/Transaction/PublicVoucher/PrivateVoucher` 相关逻辑
2. 对外提供幂等接口：
   - 钱包扣款/退款
   - 券核销/回滚
3. 订单服务通过服务调用接入资金与券能力

**验收标准**：

1. 充值/提现/领券正常
2. 下单支付成功且账实一致
3. 取消订单后钱包与券状态回滚正确

---

### 阶段 4：拆分 `catalog-service`（2026-04-17 ~ 2026-04-24）

**目标**：商家与菜品独立，库存通过服务接口协同。

**任务**：

1. 迁移商家、菜品、申请审核相关模块
2. 提供库存扣减与回补接口
3. 订单服务改为远程调用库存能力

**验收标准**：

1. 商家端店铺/菜品管理功能正常
2. 下单库存扣减正确
3. 取消订单库存回补正确

---

### 阶段 5：拆分 `order-service` 与总切换（2026-04-25 ~ 2026-05-01）

**目标**：完成订单域独立部署并整体切流。

**任务**：

1. 订单、地址、购物车、评价、订单明细迁移完成
2. 统一网关路由切换至五服务部署
3. 完成跨服务联调与压测

**验收标准**：

1. 订单生命周期全链路通过：
   `创建 -> 支付 -> 接单/配送 -> 完成 -> 评价 -> 取消（可回滚）`
2. 异步事件不丢失、可重试、可审计
3. 对外 API 无破坏性变更（或已完成版本化）

---

## 3. 数据库拆分策略（当前库无历史数据）

采用“按服务直接建新 schema”的方式，不做历史回填迁移。

1. 同一 MySQL 实例下创建多 schema：
   - `elm_iam`
   - `elm_points`
   - `elm_account`
   - `elm_catalog`
   - `elm_order`
2. 每个服务独立维护 DDL（建议 Flyway/Liquibase）
3. 每个阶段只切一个域，保留旧表一版作为快速回滚兜底

---

## 4. 关键技术策略

### 4.1 身份与权限

1. JWT 标准化：至少包含 `sub`、`uid`、`auth`、`exp`
2. 业务服务只信任网关透传 token（或网关解析后透传签名上下文）
3. 内部服务调用继续使用内部 token（后续可升级 mTLS）

### 4.2 一致性与幂等

1. 资金、库存、积分相关接口必须支持幂等键（`requestId/orderId`）
2. 跨服务事件统一走 Outbox，禁止关键链路“本地事务 + 远程同步强依赖”
3. 失败补偿路径明确：扣款失败、发券失败、发积分失败均可重试或人工补偿

### 4.3 可观测性

1. 日志统一字段：`traceId/spanId/requestId/userId/orderId`
2. Outbox 监控：`PENDING/RETRY/FAILED` 数量与重试次数
3. 服务健康检查、慢查询、错误率告警

---

## 5. 里程碑与交付物

每阶段交付物固定如下：

1. 架构变更说明（边界、调用链、数据归属）
2. 配置模板（环境变量、路由、token、数据库）
3. 数据库初始化脚本（或迁移脚本）
4. 回归测试清单与结果
5. 回滚方案（切流失败时的恢复步骤）

---

## 6. 风险清单与优先级

### P0（必须先解决）

1. 非 IAM 域对 `User` 实体强依赖
2. 钱包/库存操作缺少统一幂等键
3. 跨服务同步调用引发的“主流程成功、子流程失败”不一致

### P1（并行推进）

1. 接口契约未版本化导致前端联调风险
2. 测试覆盖不足导致拆分后回归成本高
3. 内部 token 管理与轮换机制不完善

---

## 7. 完成定义（Definition of Done）

满足以下条件即视为拆分完成：

1. 五个服务独立构建、独立部署、独立扩容
2. 数据库按服务独立 schema，禁止跨服务直连对方表
3. 订单主链路与补偿链路在压测下稳定
4. API 文档、运维手册、故障处理手册齐全
