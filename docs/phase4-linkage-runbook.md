# 阶段4联调Runbook（elm-v2.0 + catalog-service）

## 1. 启动服务

1. 启动 `catalog-service`（端口 `8083`）
   - `cd elm-microservice/catalog-service`
   - `DB_URL='jdbc:mysql://localhost:3306/elm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' DB_USERNAME='user' DB_PASSWORD='pass@WORD' mvn spring-boot:run`
2. 启动单体 `elm-v2.0`（端口 `8080`，指向 `catalog-service`）
   - `cd elm-v2.0`
   - `DB_USERNAME='user' DB_PASSWORD='pass@WORD' CATALOG_SERVICE_URL='http://localhost:8083/elm' ACCOUNT_SERVICE_URL='http://localhost:8082/elm' POINTS_SERVICE_URL='http://localhost:8081/elm' mvn spring-boot:run`

## 2. 核心链路 Smoke

1. 用户登录，拿 JWT
   - `POST /elm/api/auth`
2. 地址准备
   - `GET /elm/api/addresses`，为空则 `POST /elm/api/addresses`
3. 购物车准备
   - 选择已有 `business` 与 `food`，调用 `POST /elm/api/carts`
4. 下单（触发远程扣库存）
   - `POST /elm/api/orders`
   - Header 传 `X-Request-Id`（建议固定前缀）
5. 取消订单（触发远程回补库存）
   - `POST /elm/api/orders/{orderId}/cancel`

## 3. 联调核验点

1. 订单状态
   - 取消后 `orderState=0 (CANCELED)`
2. 目录服务库存快照
   - 下单前后对比 `GET /elm/api/inner/catalog/food/{foodId}` 的 `stock`
   - 取消后库存应回补到下单前
3. 幂等校验
   - 相同 `X-Request-Id` 重复下单，应返回同一订单
   - 同一取消请求重放，不应重复扣减或回补库存

## 4. 已知约束

1. 当前库存幂等键为 `requestId`
2. 联调建议固定 `X-Request-Id` 便于追踪 `stock_request_log`
3. 如需排查，优先查看 `catalog-service` 中 `stock_request_log` 与 `food.stock`
