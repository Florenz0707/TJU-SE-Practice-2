# 阶段5：新版方案联调 Runbook（购物车+评价迁移后）

## 1. 目的

验证新版方案在“网关 -> 聚合层 -> 微服务集群”模式下可承接登录、地址、购物车、下单、取消、状态推进、评价主链路，并保持幂等与补偿行为稳定。

## 2. 参与服务

1. `gateway-service`（8090，可选但推荐）
2. `elm-v2.0`（8080）
3. `points-service`（8081）
4. `account-service`（8082）
5. `business-service`（8083 / 8183）
6. `food-service`（8087 / 8187）
7. `cart-service`（8089 / 8189）
8. `order-service`（8084 / 8184）
9. `address-service`（8085）
10. `user-service`（8086）

## 3. 前置条件

1. 根目录 `.env` 已存在，至少配置：`DB_USERNAME`、`DB_PASSWORD`、`INTERNAL_SERVICE_TOKEN`
2. 所有参与服务均已启动并返回健康响应
3. 至少满足以下内部接口可用：
   - `GET /elm/api/inner/order/ping`
   - `GET /elm/api/inner/address/list/{userId}`
   - `GET /elm/api/inner/cart/list`
4. 脚本环境准备完成：
   - `cd elm-v2.0/scripts`
   - `cp integration.env.example .env`
   - `uv sync`

## 4. 执行步骤

### 4.1 Docker Compose 模式

1. 先确认当前终端具备 Docker 运行条件：
   - `docker compose version`
   - 如果你当前在 dev container 中开发，出现 `docker: command not found` 或无法连接 daemon，说明必须切回宿主机终端执行 compose
2. 启动整套容器环境：`docker compose up -d --build`
   - 如果首次一键启动后出现配置中心、注册中心或下游服务级联重启，改用分阶段启动：
   - `docker compose up -d --build mysql mysql-init config-server`
   - `docker compose up -d --build discovery-server-a discovery-server-b`
   - `docker compose up -d --build points-service account-service business-service-a business-service-b food-service-a food-service-b cart-service-a cart-service-b order-service-a order-service-b address-service user-service elm-v2 gateway-service frontend`
   - 每阶段完成后分别检查 `http://localhost:8888/actuator/health`、`http://localhost:8761`、`http://localhost:8090/actuator/health`
3. 可选检查网关与聚合层：
   - 网关：`http://localhost:8090/actuator/health`
   - 前端：`http://localhost`
   - 聚合层：`http://localhost:8080/swagger-ui/index.html`
4. 执行脚本化联调：
   - `cd elm-v2.0/scripts && uv run run_four_service_smoke.py --env-file .env --skip-start`
5. 校验订单链路：
   - 下单成功、取消成功、完成态更新成功
6. 校验购物车链路：
   - 下单前购物车读取成功
   - 下单后购物车清理成功（无遗留脏数据）
7. 校验评价链路：
   - 评价新增后订单状态从 `COMPLETE` 迁移到 `COMMENTED`
   - 删评后订单状态回滚到 `COMPLETE`
8. 校验 outbox：
   - `POINTS_ORDER_SUCCESS` 事件状态应为 `SENT`
9. 前端联调直接访问容器化入口：
   - 前端默认通过 Nginx 转发到 `gateway-service`
   - 浏览器访问 `http://localhost`

### 4.2 本地直跑模式（不使用 Docker Compose）

1. 启动配置中心、注册中心、网关：
   - `bash scripts/run-local-cloud.sh`
2. 启动业务服务与聚合层：
   - `bash scripts/run-local-backend-cloud.sh`
3. 检查关键健康接口：
   - `http://localhost:8090/actuator/health`
   - `http://localhost:8080/elm/actuator/health`
4. 若只需要单独重启聚合层，使用仓库自带工具链：
   - `cd elm-v2.0`
   - `export JAVA_HOME=/root/workspace/TJU-SE-Practice-2/.tools/jdk-21`
   - `export PATH="$JAVA_HOME/bin:/root/workspace/TJU-SE-Practice-2/.tools/apache-maven-3.9.9/bin:$PATH"`
   - `mvn -Dmaven.test.skip=true -Dspring-boot.run.profiles=local,cloud spring-boot:run`
5. 若需要快速重复执行纯 API 冒烟，不依赖数据库直连脚本：
   - `node elm-v2.0/scripts/run_gateway_api_smoke.mjs`
   - 可选：`node elm-v2.0/scripts/run_gateway_api_smoke.mjs --env-file elm-v2.0/scripts/gateway-api-smoke.env`
   - 默认走 `http://localhost:8090`，覆盖注册、登录、钱包充值、地址、购物车、下单、取消、完成、评价、订单查询
6. 通过网关 `http://localhost:8090` 做主链路验证，优先验证：
   - `POST /api/persons`
   - `POST /api/auth`
   - `GET /api/wallet/my`
   - `POST /api/wallet/my/topup`
   - `POST /api/addresses`
   - `POST /api/carts`
   - `POST /api/orders`
   - `POST /api/orders/{id}/cancel`
   - `PATCH /api/orders`
   - `POST /api/reviews/order/{orderId}`
   - `DELETE /api/reviews/{reviewId}`
   - `GET /api/orders/user/my`

## 5. 验收标准

1. 创建接口幂等生效：同一 `requestId` 不生成重复订单
2. 取消接口仅允许订单所属用户操作
3. 订单状态流转符合约束（含 `PAID->CANCELED`、`COMPLETE->COMMENTED`、删评回滚）
4. 购物车与地址远程调用稳定，无本地仓储兜底依赖
5. 通过网关访问时主链路无额外 5xx
6. `integration_outbox_event` 中 `POINTS_ORDER_SUCCESS` 为 `SENT`
7. 聚合层 `/api/wallet` 与 `/api/orders` 使用同一账户资金源，不需要再绕过 `/services/account/*` 直充

## 6. 回滚策略

1. 若 `order-service` 写接口异常，先切只读兜底与编排降级开关，暂停评价/删评入口
2. 若网关异常，前端可临时直连聚合层 `http://localhost:8080`
3. 保留 `order-service` 只读查询接口用于并行核对
4. 回滚后执行最小 smoke：下单、取消、查询订单、购物车读取、地址读取
5. 回滚完成后保留异常样本 `requestId/orderId/reviewId`，用于补偿复盘

## 7. 执行记录（2026-03-22，基础版）

1. 执行环境：
   - `order-service`：`8084`
   - 请求基地址：`http://localhost:8084/elm/api/inner/order`
2. 用例结果：
   - `GET /ping`：成功
   - `POST /create`：成功，生成 `orderId=1`
   - 同 `requestId` 重试 `POST /create`：返回同 `orderId=1`（幂等通过）
   - `POST /{orderId}/cancel`（owner）：成功
   - 重复取消：失败，消息 `OrderState NOT PAID`
   - 非 owner 取消：失败，消息 `AUTHORITY LACKED`
3. 结论：
   - 阶段5基础 smoke 通过，可继续推进订单读链路迁移与四服务联调

## 8. 微服务联调记录（2026-03-22）

1. 参与服务：
   - `points-service`（8081）
   - `account-service`（8082）
   - `business-service`（8083）
   - `order-service`（8084）
   - `elm-v2.0`（8080，编排入口）
2. 执行摘要：
   - 新建 smoke 用户并完成钱包/地址/购物车准备
   - 下单（钱包支付）成功：`ORDER1_OK=true`
   - 取消成功：`CANCEL1_OK=true`
   - 第二单下单成功：`ORDER2_OK=true`
   - 更新订单到完成态成功：`COMPLETE_OK=true`
3. 联调核验：
   - `account/business/food/order` 取消链路成功闭环
   - `integration_outbox_event` 新增 `POINTS_ORDER_SUCCESS` 且状态 `SENT`，确认 points 通道联通

## 9. 四服务联调复验（购物车+评价迁移后，2026-03-22）

1. 执行命令：
   - `cd elm-v2.0/scripts && uv run run_four_service_smoke.py --env-file .env`
2. 样例输出：
   - `SMOKE_OK=true`
   - `ORDER1_ID=10`
   - `ORDER2_ID=11`
   - `CANCEL1_STATE=0`
   - `COMPLETE2_STATE=4`
   - `OUTBOX_EVENT_TYPE=POINTS_ORDER_SUCCESS`
   - `OUTBOX_STATUS=SENT`
3. 结论：
   - 阶段5迁移链路可继续推进边界态回归与回滚脚本核对

## 10. 本地直跑复验（2026-03-31）

1. 运行方式：
   - 不使用 `docker compose`
   - 使用 `scripts/run-local-cloud.sh`、`scripts/run-local-backend-cloud.sh`
   - 单独重启 `elm-v2.0` 时使用仓库内 `.tools/jdk-21` 与 `.tools/apache-maven-3.9.9`
2. 关键修复：
   - 用户注册内部调用已显式透传 `password`，避免注册成功但实际无法登录
   - 聚合层 `WalletServiceImpl`、`TransactionServiceImpl` 已改为通过 `InternalAccountClient` 调用 `account-service`
   - `/api/wallet/my/topup` 后可直接 `/api/orders` 下单，不再需要走 `/services/account/api/wallet/*` 绕过
3. 实际样本：
   - 用户：`wallet_smoke_1774945185`
   - `userId=37`
   - `walletId=37`
   - 商家：`businessId=1`
   - 菜品：`foodId=1`
4. 验证结果：
   - 钱包初始余额：`0.0`
   - 聚合层充值 `100.00` 成功，余额变为 `100.0`
   - 首单 `walletPaid=28.00` 创建成功，取消后订单状态为 `0`，余额回到 `100.0`
   - 第二单 `walletPaid=28.00` 创建成功，完成后订单状态为 `4`
   - 评价新增、查询、删除成功
   - `GET /api/orders/user/my` 返回 `2` 条订单
   - 最终钱包余额为 `72.0`
5. 结论：
   - 非 compose 本地直跑下，登录、地址、购物车、钱包、下单、取消、完成、评价主链路已闭环
   - 当前最可信入口仍是网关 `http://localhost:8090`

## 11. 本地直跑复验补充（2026-03-31，扩展验证）

1. 本轮新增验证范围：
   - 在同一套本地直跑服务上，同时完成新增自动化测试后的真实 API 冒烟回归
   - 验证注册、登录、钱包、地址、购物车、下单、取消、完成、评价、订单查询链路未因测试补充和前端改动回退
2. 实际样本：
   - 用户：`smoke_1774954930140`
   - `userId=39`
   - `walletId=38`
   - `addressId=10`
   - `order1Id=8`
   - `order2Id=9`
   - `reviewId=4`
3. 验证结果：
   - 注册和登录成功
   - 钱包初始余额：`0`
   - `POST /api/wallet/my/topup` 充值 `100` 成功，余额变为 `100`
   - 首单创建后状态为 `1`，取消后状态为 `0`
   - 第二单创建成功，`PATCH /api/orders` 后状态为 `4`
   - 评价新增、按订单查询、删除均成功
   - `GET /api/orders/user/my` 返回 `2` 条订单
   - 最终钱包余额为 `72`
4. 结论：
   - 新增 `user-service/address-service/cart-service` 控制器测试以及前端 `Vitest` 测试后，真实业务链路仍保持可用
   - 当前本地非 compose 方案已同时具备自动化测试基线和真实主链路回归结果

## 12. 可重复执行的网关 API 冒烟脚本（2026-03-31）

1. 脚本位置：
   - `elm-v2.0/scripts/run_gateway_api_smoke.mjs`
   - 环境模板：`elm-v2.0/scripts/gateway-api-smoke.env.example`
   - 根级快捷入口：`pnpm --dir . run smoke:gateway-api`
2. 默认行为：
   - 默认网关地址：`http://localhost:8090`
   - 自动创建新用户并完成注册、登录、钱包充值、地址新增、购物车新增、两次下单、首单取消、第二单完成、评价增删查、我的订单查询
3. 最近一次执行样本：
   - 用户：`smoke_1774955296733`
   - `userId=40`
   - `order1Id=10`
   - `order2Id=11`
   - `reviewId=5`
4. 最近一次执行结果：
   - `cancel1State=0`
   - `complete2State=4`
   - `orderCount=2`
   - `walletFinal=72`
5. 结论：
   - 该脚本可作为当前本地非 compose 方案的标准快速冒烟入口
