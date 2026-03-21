# 阶段3联调Runbook（elm-v2.0 + account-service）

## 1. 启动服务

1. 启动 `account-service`（端口 `8082`）
   - `cd elm-microservice/account-service`
   - `DB_URL='jdbc:mysql://localhost:3306/elm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' DB_USERNAME='user' DB_PASSWORD='pass@WORD' mvn spring-boot:run`
2. 启动单体 `elm-v2.0`（端口 `8080`，指向 `account-service`）
   - `cd elm-v2.0`
   - `DB_USERNAME='user' DB_PASSWORD='pass@WORD' ACCOUNT_SERVICE_URL='http://localhost:8082/elm' POINTS_SERVICE_URL='http://localhost:8080/elm' mvn spring-boot:run`

## 2. 核心链路 Smoke

1. 用户登录，拿 JWT：
   - `POST /elm/api/auth`（`user/password`）
2. 钱包准备：
   - `POST /elm/api/wallet`（若已存在可忽略失败）
   - `POST /elm/api/wallet/my/topup`
3. 地址准备：
   - `GET /elm/api/addresses`，若为空则 `POST /elm/api/addresses`
4. 购物车准备：
   - 选择已有 `business` 与 `food`，调用 `POST /elm/api/carts`
5. 优惠券准备：
   - 管理员 `POST /elm/api/publicVoucher`
   - 用户 `POST /elm/api/privateVoucher/claim/{publicVoucherId}`
6. 下单（钱包 + 券）：
   - `POST /elm/api/orders`
   - Header 传 `X-Request-Id`（建议固定前缀，便于追踪）
7. 取消订单：
   - `POST /elm/api/orders/{orderId}/cancel`

## 3. 联调核验点

1. 订单状态：
   - 取消后 `orderState=0 (CANCELED)`
2. 券状态：
   - `GET /elm/api/inner/account/voucher/{voucherId}`
   - 下单后 `deleted=true`
   - 取消后 `deleted=false`
3. 交易状态：
   - `GET /elm/api/inner/account/transaction/by-biz/{requestId}`（下单扣款）
   - `GET /elm/api/inner/account/transaction/by-biz/ORDER_{orderId}`（取消退款）
4. 钱包余额：
   - `GET /elm/api/wallet/my`
   - 下单后减少，取消后回补

## 4. 异常补偿演练建议

1. 模拟 `account-service` 暂时不可达：
   - 下单应失败，不应落订单
2. 模拟券回滚失败：
   - 取消订单应返回失败并保持原状态，避免资金券不一致
3. 检查幂等：
   - 重复调用同一 `X-Request-Id` 的下单请求，应返回同一订单

## 5. 已验证样例（2026-03-21）

1. `REQ_ID=smoke-order-1774103637`
2. `ORDER_ID=3`
3. 取消后状态：`0`
4. 下单后券：`deleted=true`，取消后：`deleted=false`
5. `transaction/by-biz` 可分别查到下单扣款与取消退款记录
