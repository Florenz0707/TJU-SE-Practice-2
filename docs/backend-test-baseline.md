# 后端测试基线（2026-04-01）

## 1. 目的

沉淀当前新版方案在 2026-04-01 的后端测试与验证状态，区分：

1. 已有自动化测试且通过的模块
2. 当前可构建但暂无测试用例的模块
3. 已完成的真实业务回归范围
4. 下一步优先补测方向

## 2. 自动化测试现状

### 2.1 已执行并通过

1. `elm-v2.0`
   - 结果：全量测试通过
   - 说明：已修复 `OrderApplicationServiceTest`、`TransactionServiceImplTest`、`MyApplicationTests`、`PrivateVoucherClaimIntegrationTest`
   - 补充：2026-03-31 已继续扩 `OrderApplicationServiceTest`，新增 requestId 幂等返回、钱包加载失败、钱包支付超过剩余应付金额的前置拦截、优惠券过期、优惠券门槛不满足、优惠券归属错误、优惠券核销失败、积分抵扣超限、商家未营业、商家不可用、地址不存在、地址归属错误、购物车为空、菜品不存在、菜品不可用、菜品归属错误、菜品 ID 缺失、菜品价格缺失、库存不足、低于起送价、积分冻结失败、积分扣减失败、取消订单的订单不存在/越权/非已支付状态拒绝、积分退款失败回退到 rollbackPoints、取消订单优惠券回滚失败、状态更新的空请求/缺失订单 ID/订单不存在/缺失状态/非法状态/未授权拒绝，以及管理员路径、用户本人路径、订单完成积分出站通知与出站异常不影响主流程等边界与补偿测试
   - 补充：2026-03-31 已修复 `OrderApplicationService` 一处真实缺陷：`freezePoints` 返回空时不再直接短路返回，而会进入统一异常补偿流程，正确回滚钱包、优惠券与库存预占
   - 补充：2026-03-31 已补 `PrivateVoucherControllerTest` 并单独执行通过，覆盖领取公共优惠券、查询我的优惠券、核销优惠券的未登录、依赖对象缺失、成功和异常分支
   - 补充：2026-03-31 已继续扩 `OrderControllerTest`，新增 `updateOrderStatus` 在 `ADMIN` 和普通 `USER` 角色下的角色标志透传分支
   - 补充：2026-03-31 已新增 `WalletControllerTest`、`PublicVoucherControllerTest`、`TransactionControllerTest` 与 `PointsControllerTest`，覆盖钱包查询/创建/充值/提现/加券、公共优惠券基础校验和可领取列表过滤、交易入口的当前用户钱包查询/越权校验/交易类型与支付钱包参数校验/管理员完成交易透传，以及积分账户与积分明细分页的登录态、账户缺失、成功和异常映射
   - 补充：2026-03-31 已新增 `AuthenticationRestControllerTest` 与 `UserRestControllerTest`，覆盖登录成功时 JWT body/header 返回、下游鉴权失败返回 401，以及用户创建默认字段/密码加密、修改密码、自查/查人权限和软删除分支
   - 补充：2026-03-31 已新增 `PointsAdminControllerTest`、`MerchantApplicationControllerTest` 与 `BusinessApplicationControllerTest`，覆盖积分规则新增/更新/删除的管理员权限与异常映射，以及商家申请、开店申请的提交、查询、审批状态流转和审批通过后的用户权限升级、店铺上线副作用
   - 补充：2026-03-31 已新增 `BusinessControllerTest`、`FoodControllerTest` 与 `AddressControllerTest`，覆盖店铺/菜品/地址入口的查询参数校验、所有权与角色校验、替换更新时的字段回填，以及软删除和内部地址服务失败映射
   - 补充：2026-03-31 已继续扩 `OrderApplicationServiceTest`，新增库存预占失败时钱包退款与优惠券回滚双补偿、订单持久化失败且积分已扣减时触发 `refundDeductedPoints`、空白 `requestId` 自动生成 `order-create-*` 请求标识、取消订单在缺失 `pointsTradeNo` 时回退使用 `ORDER_<id>` 做积分补偿、取消订单钱包退款失败拦截、取消订单远端状态更新失败，以及完单积分发放净额计算与折扣超过订单金额时归零等编排边界
   - 补充：2026-03-31 已继续扩 `OrderControllerTest` 与 `ReviewApplicationServiceTest`，新增订单创建未登录拒绝、商家订单聚合查询、商家非店主越权拒绝、店铺订单查询缺失鉴权，以及删评时积分通知异常容错、删评失败、管理员删评、删评后订单状态回滚失败等边界；同时修复 `ReviewApplicationService.deleteReview` 一处真实缺陷：删评后若订单状态未成功从 `COMMENTED` 回退到 `COMPLETE`，现在会返回明确失败而不再静默成功
   - 补充：2026-03-31 已继续扩 `ReviewControllerTest` 与 `ReviewApplicationServiceTest`，新增评价创建未登录拒绝、查询我的评价未登录拒绝、更新评价缺失鉴权/评价不存在/远端更新失败、按订单查评价的缺失鉴权/订单不存在/本人查看/商家不存在/普通用户越权，以及按店铺查评价的店铺不存在、删除评价的未登录拒绝和管理员标志透传；同时继续补 `ReviewApplicationService.addReview` 的订单不存在、评价体为空、评分缺失、创建评价失败、订单状态回写失败和 outbox 异常容错分支
   - 最近一次全量执行结果：`Tests run: 241, Failures: 0, Errors: 0, Skipped: 0`
   - 最近一次 `OrderApplicationServiceTest` 专项执行结果：`Tests run: 58, Failures: 0, Errors: 0, Skipped: 0`
   - 最近一次入口专项执行结果：`WalletControllerTest` `10` 条、`PublicVoucherControllerTest` `6` 条、`TransactionControllerTest` `9` 条、`PointsControllerTest` `7` 条、`AuthenticationRestControllerTest` `3` 条、`UserRestControllerTest` `12` 条、`PointsAdminControllerTest` `6` 条、`MerchantApplicationControllerTest` `6` 条、`BusinessApplicationControllerTest` `6` 条、`BusinessControllerTest` `6` 条、`FoodControllerTest` `6` 条、`AddressControllerTest` `6` 条、`OrderControllerTest` `25` 条、`ReviewControllerTest` `23` 条、`ReviewApplicationServiceTest` `16` 条，均全部通过
   - 测试环境要求：`local` profile，禁用 config/eureka，使用本地 H2
2. `elm-microservice/account-service`
   - 结果：全量测试通过
   - 补充：2026-03-31 已继续补 `AccountInternalServiceTest` 与 `AccountInnerControllerTest`，覆盖钱包扣款参数校验、缺失钱包、退款幂等、优惠券核销与回滚的归属/已删除/恢复分支，以及 controller 对失败与异常的返回映射
   - 补充：2026-04-01 已新增 `AccountInnerControllerSecurityTest`，覆盖内部账户接口在缺失 token、错误 token 时返回 401，以及合法 `X-Internal-Service-Token` 时放行到内部钱包查询入口
   - 最近一次执行结果：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`
3. `elm-microservice/order-service`
   - 结果：`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`
4. `elm-microservice/points-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：积分账户自动创建、积分冻结、订单取消返还积分、退款幂等、冻结积分回滚失败
   - 补充：2026-04-01 已继续扩 `PointsServiceTest`，新增已返还积分的幂等短路，以及冻结批次缺失时 `rollbackPoints` 返回明确失败
   - 最近一次执行结果：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
5. `elm-microservice/gateway-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：配置刷新接口的鉴权拒绝与聚合逻辑基础行为
   - 补充：2026-03-31 已补路由重写集成测试并通过，验证 `/api/** -> /elm/api/**` 重写、`/elm/**` 直通和 `/services/address/** -> /elm/**` 直连服务重写
   - 补充：2026-03-31 已继续扩 `GatewayRouteRewriteIntegrationTest`，新增 `/v3/api-docs -> /elm/v3/api-docs` 和 `/services/user/profile -> /elm/profile` 两条路由重写断言，补齐 OpenAPI 与 user-service 直连路由覆盖
   - 补充：2026-03-31 已继续扩 `ConfigRefreshControllerTest`，新增配置刷新时成功实例与失败实例混合场景下的结果聚合、成功计数和错误信息回传断言，避免测试依赖异步返回顺序
   - 补充：2026-04-01 已增强 `ConfigRefreshController` 的服务发现异常兜底，并新增测试覆盖服务列表获取失败时返回结构化 `503`、单个服务实例发现失败时仍以失败结果聚合返回
   - 最近一次执行结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`
6. `elm-microservice/business-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：商家快照列表过滤、按 ID 查询的存在/空值场景
   - 补充：2026-03-31 已补 `BusinessInnerControllerTest` 并通过，覆盖内部商家列表与按 ID 查询成功/缺失分支
   - 最近一次执行结果：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
7. `elm-microservice/food-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：商家校验、库存预占、库存回补、重复 `requestId` 幂等返回、失败 requestId 重放、数量非法、商家不存在、已删除菜品拦截、批量库存操作一致性、并发预占保护
   - 补充：2026-03-31 已继续扩 `FoodInnerControllerTest`、`FoodInternalServiceTest` 与并发集成测试，新增库存预占失败映射、库存回补空请求/成功映射，以及库存 service 层对失败幂等、数量非法、商家校验失败、已删除菜品、批量成功扣减、批量校验失败时不提前写库存、并发竞争下最后一件商品仅允许一次成功预占等分支
   - 补充：2026-03-31 已在 `FoodRepository` / `FoodInternalService` 中引入悲观写锁读取，修复库存并发预占可能超卖的实现风险
   - 补充：2026-03-31 已继续扩 `FoodInternalServiceTest`，新增库存回补的缺失 `requestId`、数量缺失、商家校验失败以及批量回补校验失败时整体不落库等边界
   - 最近一次执行结果：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`
8. `elm-microservice/cart-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：购物车创建、数量更新、用户查询、删除
9. `elm-microservice/address-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：地址创建、更新、按用户查询、逻辑删除
10. `elm-microservice/config-server`
   - 结果：2026-03-31 已补最小启动测试并通过
   - 覆盖重点：`native` profile 下配置中心上下文加载
11. `elm-microservice/discovery-server`
   - 结果：2026-03-31 已补最小启动测试并通过
   - 覆盖重点：禁用外部 config 依赖后的注册中心上下文加载
12. `elm-microservice/user-service`
   - 结果：2026-03-31 已补领域服务单元测试并通过
   - 覆盖重点：用户名规范化、重复用户名拒绝、密码编码、默认权限、钱包初始化调用、删除数据过滤
   - 补充：2026-03-31 已补控制器测试并通过，覆盖新增用户默认字段、密码修改权限、按用户查询权限、删除逻辑
   - 补充：2026-03-31 已补认证控制器测试并通过，覆盖 `/api/auth` 的 JWT header/body 返回和 SecurityContext 写入
   - 补充：2026-03-31 已补 `JWTFilter` 测试并通过，覆盖有效 Bearer Token 写入认证上下文和无效 Token 放行但不注入认证
   - 补充：2026-03-31 已补 `TokenProvider` 测试并通过，覆盖 create/validate/getUserId/getAuthentication 往返
   - 补充：2026-03-31 已新增 `UserInnerControllerTest`，覆盖内部接口在缺失 token、错误 token 时返回 401，以及合法 `X-Internal-Service-Token` 时放行到 `UserInnerController`
   - 最近一次执行结果：`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`

### 2.2 当前构建成功但暂无测试源码

当前已执行测试扫描的模块中，已不再存在“完全无测试源码”的核心后端模块。

仍需补强的是测试深度，而不是从 0 到 1 的测试基线。

说明：上述模块执行 `mvn test` 时为 `BUILD SUCCESS`，但日志显示 `No tests to run.`。当前结论是“缺测试”，不是“有失败测试”。

## 3. 真实业务回归现状

通过本地直跑方式，已完成以下主链路真实回归：

1. 注册
2. 登录
3. 钱包查询
4. 钱包充值
5. 地址新增
6. 购物车新增
7. 下单
8. 取消订单
9. 完成订单
10. 评价新增
11. 评价查询
12. 评价删除
13. 我的订单查询

关键结论：

1. 聚合层 `/api/wallet` 与 `/api/orders` 现在已经共用 `account-service` 资金源
2. 标准聚合接口可以直接完成充值后下单，不再需要 `/services/account/*` 直通绕过
3. 当前最可信的外部验证入口仍是 `http://localhost:8090`

## 4. 执行记录摘要

2026-04-01 最近一轮扩大验证包含：

1. `elm-v2.0` 全量测试通过，最近一次结果为 `241` 个测试全部通过
2. `account-service` 全量测试通过，最近一次结果为 `26` 个测试全部通过
3. `order-service` 40 个测试全部通过
4. `points-service`、`business-service`、`food-service`、`cart-service`、`address-service`、`gateway-service`、`config-server`、`discovery-server`、`user-service` 已执行 `mvn test`
5. 本轮已把 `points-service`、`gateway-service`、`business-service`、`food-service`、`cart-service`、`address-service`、`config-server`、`discovery-server`、`user-service` 从“无测试或无基线”推进到至少有最小测试可执行状态
6. `gateway-service` 最近一次 `mvn test` 结果为 `10` 个测试全部通过，新增覆盖配置刷新时成功实例与失败实例混合返回的聚合结果、成功计数与错误信息透出，以及 OpenAPI、user-service 直连路由 rewrite、服务发现整体失败时的结构化 `503` 和单个服务实例发现失败时的失败聚合
7. `user-service` 最近一次 `mvn test` 结果为 `15` 个测试全部通过，新增覆盖内部接口的 `X-Internal-Service-Token` 缺失/错误拒绝与合法 token 放行
8. `food-service` 最近一次 `mvn test` 结果为 `26` 个测试全部通过，其中已包含库存预占/回补的 controller 映射、service 幂等/校验分支、批量库存一致性补测、回补非法输入与批量失败整体不落库，以及并发预占保护集成测试
9. `points-service` 最近一次 `mvn test` 结果为 `6` 个测试全部通过，新增覆盖订单已返还积分时的幂等短路和冻结批次缺失时的回滚失败分支
10. `account-service` 最近一次 `mvn test` 结果为 `26` 个测试全部通过，新增覆盖内部账户接口的 `X-Internal-Service-Token` 缺失/错误拒绝与合法 token 放行
11. `business-service` 最近一次 `mvn test` 结果为 `6` 个测试全部通过，其中已包含新增 `BusinessInnerControllerTest`
12. `elm-v2.0` 最近一次专项执行 `PrivateVoucherControllerTest`，`11` 个测试全部通过
13. `elm-v2.0` 最近一次专项执行 `OrderApplicationServiceTest`，`58` 个测试全部通过；在既有 requestId 幂等返回、wallet 加载失败、wallet 超额拦截、voucher 过期/门槛不满足/归属错误/核销失败、points 抵扣超限、business 未营业/不可用、address 不存在/归属错误、cart 为空、food 不存在/不可用/归属错误、foodId 缺失、foodPrice 缺失、库存不足、低于起送价、freeze points 失败与 points 扣减失败回滚、取消订单的订单不存在/越权/非已支付状态拒绝、积分退款失败回退到 rollbackPoints、取消订单优惠券回滚失败、状态更新空请求/缺失订单 ID/订单不存在/缺失状态/非法状态/未授权拒绝、管理员路径、用户本人路径，以及完成订单时积分出站通知与出站异常容错基础上，本轮继续新增库存预占失败时钱包退款与优惠券回滚双补偿、订单持久化失败且积分已扣减时触发 `refundDeductedPoints`、空白 `requestId` 自动生成 `order-create-*` 请求标识、取消订单缺失 `pointsTradeNo` 时的积分补偿 fallback、取消订单钱包退款失败、取消订单远端状态更新失败，以及完单积分发放净额计算和折扣超过订单金额时归零等边界
14. `elm-v2.0` 最近一次专项执行 `OrderControllerTest`，`25` 个测试全部通过；在既有 `updateOrderStatus` 的 `ADMIN` 和普通 `USER` 角色标志透传基础上，本轮继续新增订单创建未登录拒绝、商家订单列表查询的非商家拒绝、跨店主商家越权拒绝、商家多店铺订单聚合，以及按店铺查单的缺失鉴权分支
15. `elm-v2.0` 最近一次专项执行 `WalletControllerTest`、`PublicVoucherControllerTest`、`TransactionControllerTest` 与 `PointsControllerTest`，分别 `10`、`6`、`9`、`7` 个测试全部通过，新增覆盖资产入口的钱包、优惠券、交易和积分 controller 参数校验、越权分支与异常映射
16. `elm-v2.0` 最近一次专项执行 `AuthenticationRestControllerTest` 与 `UserRestControllerTest`，分别 `3` 个和 `12` 个测试全部通过，新增覆盖登录响应头/体、鉴权失败，以及用户创建、改密、查询和软删除等安全入口分支
17. `elm-v2.0` 最近一次专项执行 `PointsAdminControllerTest`、`MerchantApplicationControllerTest` 与 `BusinessApplicationControllerTest`，各 `6` 个测试全部通过，新增覆盖积分规则后台入口的管理员权限和异常映射，以及商家申请/开店申请的权限、状态流转和审批副作用
18. `elm-v2.0` 最近一次专项执行 `BusinessControllerTest`、`FoodControllerTest` 与 `AddressControllerTest`，各 `6` 个测试全部通过，新增覆盖店铺/菜品/地址入口的查询参数校验、所有权校验、替换更新字段回填、软删除，以及内部地址服务失败映射
19. `elm-v2.0` 最近一次专项执行 `ReviewApplicationServiceTest`，`16` 个测试全部通过；在既有评价创建、重复评价、越权拒绝、删评成功基础上，本轮继续新增 `addReview` 的订单不存在、评价体为空、评分缺失、创建评价失败、订单状态回写失败，以及评价积分 outbox 异常容错，同时保留删评时积分通知异常容错、删评返回空失败、管理员删除他人评价，以及删评后订单状态回滚失败返回可观测错误。对应地，`ReviewApplicationService.deleteReview` 已修复为在订单状态回滚失败时不再静默成功
20. `elm-v2.0` 最近一次专项执行 `ReviewControllerTest`，`23` 个测试全部通过；在既有评价查询、匿名评价对商家隐藏、评价更新和店铺评价匿名字段擦除基础上，本轮继续新增评价创建未登录拒绝、我的评价未登录拒绝、更新评价缺失鉴权/评价不存在/远端更新失败、按订单查评价的缺失鉴权/订单不存在/本人查看/商家不存在/普通用户越权，以及按店铺查评价的店铺不存在、删除评价的未登录拒绝和管理员标志透传

## 5. 下一步补测优先级

建议按下面顺序继续补测试：

1. `elm-v2.0`
   - 后续应把重心转到更深的聚合编排边界，优先补订单提交时优惠券/钱包/积分联动的异常、补偿和幂等测试
2. `food-service`
   - 后续可继续补更细的下游异常映射测试，或继续扩大并发场景到多商品混合预占/回补
3. `business-service`
   - 后续可继续补更多内部接口与异常映射测试
4. `gateway-service`
   - 后续可补 WebFlux 路由级测试，而不仅是当前最小单测
5. `config-server`
   - 后续可补配置仓库目录与 profile 组合的装配断言
6. `discovery-server`
   - 后续可补 actuator 或 Eureka registry 相关集成断言

## 6. 使用建议

1. 继续新增 `@SpringBootTest` 时，优先显式禁用 config/eureka，并使用本地 profile
2. 优先补 service/controller 的最小稳定测试，不要一开始就引入跨服务集成依赖
3. 先把“无测试模块”补到至少有 smoke 或核心单测，再考虑扩大覆盖深度
4. 当前项目的 `HttpResult.code` 是字符串枚举，例如 `OK`、`NOT_FOUND`、`GENERAL_ERROR`；补 controller 断言时不要按整数状态码假设

## 7. 前端自动化测试现状

1. `elm-frontend` 已于 2026-03-31 引入 `Vitest` 最小测试基线
2. 当前已覆盖工具层、状态/路由、组件和页面基础交互：查询串拼装、时间格式化、移动端检测、服务降级状态维护、图片 URL 处理、`auth store`、`router guard`、`ServiceDegradeNotice`、`AddressCard`、`RestaurantCard`、`Login` 页面登录跳转逻辑、移动端 `Cart` 页面挂载取数/空态跳转/数量更新/去结算逻辑、`Checkout` 页面空购物车保护、初始化数据加载/订单汇总渲染、提单成功跳转、钱包余额不足拦截、未选择地址拦截、购物车信息不完整拦截、下单失败提示、积分关闭后自动清零、优惠后积分上限裁剪、优惠券仅保留未使用且未过期数据、优惠券阈值不满足时不生效、优惠券加载失败提示、积分账户加载失败提示、支付方式切换时钱包金额联动、优惠后 mixed payment 拆分联动、钱包支付 payload、优惠券 + 积分 + mixed payment 组合支付 payload、地址保存与删除分支、地址保存失败与删除失败分支，以及 `RestaurantDetail` 页面数据加载、菜单/评价渲染、点击加购和无效餐厅 ID 错误分支
3. 最近一次执行结果：`pnpm test:run` 通过，`13` 个测试文件、`54` 个测试全部通过
4. 根目录统一入口 `pnpm run test:frontend:all` 已验证通过，会顺序执行前端测试与生产构建
5. `pnpm build` 同步通过，期间已修复 `useWebSocket` 中 `setTimeout` 返回类型与 TS 环境不兼容的问题
6. 当前前端已进入页面交互级测试阶段，后续可继续补 `RestaurantDetail` 的异常分支或更多购物车联动场景，以及 `Checkout` 的更多边界条件和支付联动场景