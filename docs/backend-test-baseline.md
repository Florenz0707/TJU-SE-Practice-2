# 后端测试基线（2026-03-31）

## 1. 目的

沉淀当前新版方案在 2026-03-31 的后端测试与验证状态，区分：

1. 已有自动化测试且通过的模块
2. 当前可构建但暂无测试用例的模块
3. 已完成的真实业务回归范围
4. 下一步优先补测方向

## 2. 自动化测试现状

### 2.1 已执行并通过

1. `elm-v2.0`
   - 结果：全量测试通过
   - 说明：已修复 `OrderApplicationServiceTest`、`TransactionServiceImplTest`、`MyApplicationTests`、`PrivateVoucherClaimIntegrationTest`
   - 测试环境要求：`local` profile，禁用 config/eureka，使用本地 H2
2. `elm-microservice/account-service`
   - 结果：全量测试通过
3. `elm-microservice/order-service`
   - 结果：`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`
4. `elm-microservice/points-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：积分账户自动创建、积分冻结、订单取消返还积分
5. `elm-microservice/gateway-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：配置刷新接口的鉴权拒绝与聚合逻辑基础行为
   - 补充：2026-03-31 已补路由重写集成测试并通过，验证 `/api/** -> /elm/api/**` 重写、`/elm/**` 直通和 `/services/address/** -> /elm/**` 直连服务重写
   - 最近一次执行结果：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
6. `elm-microservice/business-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：商家快照列表过滤、按 ID 查询的存在/空值场景
7. `elm-microservice/food-service`
   - 结果：2026-03-31 已补最小单元测试并通过
   - 覆盖重点：商家校验、库存预占、库存回补、重复 `requestId` 幂等返回
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
   - 最近一次执行结果：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`

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

2026-03-31 最近一轮扩大验证包含：

1. `elm-v2.0` 全量测试通过
2. `account-service` 全量测试通过
3. `order-service` 40 个测试全部通过
4. `points-service`、`business-service`、`food-service`、`cart-service`、`address-service`、`gateway-service`、`config-server`、`discovery-server`、`user-service` 已执行 `mvn test`
5. 本轮已把 `points-service`、`gateway-service`、`business-service`、`food-service`、`cart-service`、`address-service`、`config-server`、`discovery-server`、`user-service` 从“无测试或无基线”推进到至少有最小测试可执行状态
6. `gateway-service` 最近一次 `mvn test` 结果为 `5` 个测试全部通过
7. `user-service` 最近一次 `mvn test` 结果为 `12` 个测试全部通过

## 5. 下一步补测优先级

建议按下面顺序继续补测试：

1. `address-service`
   - 后续可在现有 service 单测基础上补内部 controller 或用户隔离断言
2. `cart-service`
   - 后续可在现有 service 单测基础上补内部 controller 或批量清理链路测试
3. `food-service`
   - 后续可在现有 service 单测基础上补内部 controller 或初始化器测试
4. `gateway-service`
   - 后续可补 WebFlux 路由级测试，而不仅是当前最小单测
5. `business-service`
   - 后续可在现有 service 单测基础上补 controller 或内部接口测试
6. `config-server`
   - 后续可补配置仓库目录与 profile 组合的装配断言
7. `discovery-server`
   - 后续可补 actuator 或 Eureka registry 相关集成断言

## 6. 使用建议

1. 继续新增 `@SpringBootTest` 时，优先显式禁用 config/eureka，并使用本地 profile
2. 优先补 service/controller 的最小稳定测试，不要一开始就引入跨服务集成依赖
3. 先把“无测试模块”补到至少有 smoke 或核心单测，再考虑扩大覆盖深度

## 7. 前端自动化测试现状

1. `elm-frontend` 已于 2026-03-31 引入 `Vitest` 最小测试基线
2. 当前已覆盖工具层、状态/路由、组件和页面基础交互：查询串拼装、时间格式化、移动端检测、服务降级状态维护、图片 URL 处理、`auth store`、`router guard`、`ServiceDegradeNotice`、`AddressCard`、`RestaurantCard`、`Login` 页面登录跳转逻辑、移动端 `Cart` 页面挂载取数/空态跳转/数量更新/去结算逻辑、`Checkout` 页面空购物车保护、初始化数据加载/订单汇总渲染、提单成功跳转、钱包余额不足拦截、未选择地址拦截、购物车信息不完整拦截、下单失败提示、积分关闭后自动清零、优惠后积分上限裁剪、优惠券仅保留未使用且未过期数据、优惠券阈值不满足时不生效、优惠券加载失败提示、积分账户加载失败提示、支付方式切换时钱包金额联动、优惠后 mixed payment 拆分联动、钱包支付 payload、优惠券 + 积分 + mixed payment 组合支付 payload、地址保存与删除分支、地址保存失败与删除失败分支，以及 `RestaurantDetail` 页面数据加载、菜单/评价渲染、点击加购和无效餐厅 ID 错误分支
3. 最近一次执行结果：`pnpm test:run` 通过，`13` 个测试文件、`54` 个测试全部通过
4. 根目录统一入口 `pnpm run test:frontend:all` 已验证通过，会顺序执行前端测试与生产构建
5. `pnpm build` 同步通过，期间已修复 `useWebSocket` 中 `setTimeout` 返回类型与 TS 环境不兼容的问题
6. 当前前端已进入页面交互级测试阶段，后续可继续补 `RestaurantDetail` 的异常分支或更多购物车联动场景，以及 `Checkout` 的更多边界条件和支付联动场景