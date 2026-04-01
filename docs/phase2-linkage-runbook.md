# 阶段2联调Runbook（单体 + points-service）

## 1. 启动服务

1. 启动 points-service
   - `cd elm-microservice/points-service && mvn spring-boot:run`
2. 启动单体（指向远程积分服务）
   - `cd elm-v2.0 && POINTS_SERVICE_URL=http://localhost:8081/elm mvn spring-boot:run`

## 2. 业务链路检查

1. 订单完成后触发积分发放
2. 评价完成后触发积分发放
3. 取消订单后积分返还/解冻

以上链路执行后，检查 points-service 的积分账户、积分记录、积分批次数据变化。

## 3. Outbox 可观测与恢复

1. 查看状态总览
   - `GET /elm/api/integration/outbox/admin/summary`
2. 手动触发一轮投递
   - `POST /elm/api/integration/outbox/admin/dispatch-now`
3. 重入队失败事件（单条）
   - `POST /elm/api/integration/outbox/admin/requeue/{eventId}`
4. 批量重入队失败事件（最多20条）
   - `POST /elm/api/integration/outbox/admin/requeue/failed`

说明：上述接口需管理员身份调用。
