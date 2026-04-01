# user-service

## 业务边界

`user-service` 负责用户域与认证域能力。

- 登录认证、JWT 签发
- 用户查询、注册、资料更新、权限维护
- 内部接口前缀：`/elm/api/inner/**`

## 当前拆分策略

- 服务已经独立部署并注册到 Eureka
- 仍复用原有 `elm` 用户表，避免一次性迁移历史用户数据
- 钱包创建通过内部调用 `account-service` 完成
- 当前保持单实例部署，符合课程验收中用户服务单实例的口径

## 与其他服务关系

- 被 `elm-v2.0` 调用，负责登录、注册、当前用户查询和内部用户快照查询
- 内部接口统一受 `X-Internal-Service-Token` 保护，缺失或错误 token 会返回 `401`

## Docker 部署（统一方式）

本服务通过根目录 `docker-compose.yml` 部署，不单独维护编排文件。

```bash
docker compose up -d --build user-service
```

默认端口：`8086`
默认上下文：`/elm`

## 运行配置

- `DB_URL`（当前仍指向共享 `elm` 用户库）
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_SERVICE_TOKEN`

## 当前实现说明

- 启动时会幂等补齐默认权限和演示账号，避免新库或 H2 本地环境登录失败
- 自动化测试已覆盖认证控制器、用户控制器、JWT 过滤器、TokenProvider 和内部 token 过滤链