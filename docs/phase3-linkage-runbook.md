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

## 5. 脚本化演练（2026-03-22）

1. phase3 自动化脚本：
   - `cd elm-v2.0/scripts && uv run run_phase3_account_drill.py --env-file .env`
2. 覆盖项：
   - 钱包扣款幂等（重复 requestId）
   - 钱包退款幂等（重复 requestId）
   - 券回滚失败分支（非法 voucherId）
   - account-service 不可达探测
3. 记录文档：
   - `docs/phase3-compensation-drill.md`

## 6. 灰度开关与回滚脚本（2026-03-22）

1. 灰度切换脚本：
   - `cd elm-v2.0/scripts && uv run manage_account_gray.py status --env-file .env`
   - `cd elm-v2.0/scripts && uv run manage_account_gray.py switch --env-file .env --mode canary --target-url http://localhost:8082/elm`
2. 回滚脚本：
   - `cd elm-v2.0/scripts && uv run rollback_account_gray.py --env-file .env`
3. 切换策略：
   - 优先执行 `status` 探针，确认目标地址内部接口可用
   - 切换后重启 `elm-v2.0` 使新配置生效
   - 失败时执行 `rollback_account_gray.py` 并重启 `elm-v2.0`
4. 相关配置键：
   - `ACCOUNT_SERVICE_URL`
   - `ACCOUNT_SERVICE_URL_PREVIOUS`
   - `ACCOUNT_GRAY_MODE`
   - `ACCOUNT_SERVICE_ROLLBACK_URL`（可选兜底）
5. 实操结论（2026-03-22）：
   - 回滚到 `http://localhost:8080/elm` 会导致下单链路失败（`Failed to load wallet`）
   - 当前阶段建议回滚到上一个可用地址（通常为 `ACCOUNT_SERVICE_URL_PREVIOUS`，示例 `http://localhost:8082/elm`）

## 7. 已验证样例（2026-03-21）

1. `REQ_ID=smoke-order-1774103637`
2. `ORDER_ID=3`
3. 取消后状态：`0`
4. 下单后券：`deleted=true`，取消后：`deleted=false`
5. `transaction/by-biz` 可分别查到下单扣款与取消退款记录
