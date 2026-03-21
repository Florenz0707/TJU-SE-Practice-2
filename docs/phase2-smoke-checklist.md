# 阶段2（points-service）Smoke清单执行记录

## 执行日期

- 2026-03-20

## 已执行并通过

1. 单体内部积分接口兼容性编译
   - 命令: `cd elm-v2.0 && mvn -DskipTests compile`
   - 结果: `BUILD SUCCESS`
2. 单体回归测试（含新增 PointsInnerController 单测）
   - 命令: `cd elm-v2.0 && mvn test`
   - 结果: `BUILD SUCCESS`，`Tests run: 18, Failures: 0, Errors: 0`
3. 独立 points-service 工程编译
   - 命令: `cd elm-microservice/points-service && mvn -DskipTests compile`
   - 结果: `BUILD SUCCESS`
4. 独立 points-service 测试框架检查
   - 命令: `cd elm-microservice/points-service && mvn test`
   - 结果: `BUILD SUCCESS`，当前 `No tests to run`

## 风格检查现状

1. 单体风格检查
   - 命令: `cd elm-v2.0 && mvn -DskipTests checkstyle:check`
   - 结果: 失败（`4116` 项，项目历史存量，不由本次改动引入）
2. points-service 风格检查
   - 命令: `cd elm-microservice/points-service && mvn -DskipTests checkstyle:check`
   - 结果: 失败（`1077` 项，迁移代码与 `sun_checks` 规则不一致）

## 待联调验证（需运行环境）

1. 订单完成发积分
2. 评价完成发积分
3. 订单取消积分返还/解冻
4. Outbox 重试与恢复可观测

建议在双服务启动后执行：

- `POINTS_SERVICE_URL=http://localhost:8081/elm` 启动单体（订单侧）
- 启动 `points-service`（端口 `8081`，context-path `/elm`）
- 按上述 4 条业务链路逐条回归

## Outbox 运维接口（新增）

1. 状态总览：`GET /elm/api/integration/outbox/admin/summary`
2. 手动触发一轮投递：`POST /elm/api/integration/outbox/admin/dispatch-now`
3. 失败事件重入队（单条）：`POST /elm/api/integration/outbox/admin/requeue/{eventId}`
4. 失败事件重入队（批量，最多20条）：`POST /elm/api/integration/outbox/admin/requeue/failed`

说明：上述接口要求管理员权限（`ADMIN`/`ROLE_ADMIN`）。
