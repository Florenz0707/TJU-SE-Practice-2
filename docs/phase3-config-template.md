# 阶段3配置模板（account-service 收口版）

## 1. 目的

统一开发/联调/回滚三类场景的关键配置键，避免环境切换时配置漂移。

## 2. 核心配置键

1. 数据库连接
   - `MYSQL_HOST`
   - `MYSQL_PORT`
   - `MYSQL_USER`
   - `MYSQL_PASSWORD`
   - `DB_MAIN`
   - `DB_ACCOUNT`
2. 服务地址
   - `GATEWAY_URL`
   - `ACCOUNT_SERVICE_URL`
   - `ACCOUNT_SERVICE_URL_PREVIOUS`
   - `ACCOUNT_SERVICE_ROLLBACK_URL`
3. 灰度与演练
   - `ACCOUNT_GRAY_MODE`
   - `PHASE3_DRILL_USER_ID`
   - `PHASE3_DRILL_AMOUNT`
4. 安全配置
   - `INTERNAL_SERVICE_TOKEN`

## 3. 推荐基线（本地联调）

1. `ACCOUNT_SERVICE_URL=http://localhost:8082/elm`
2. `ACCOUNT_SERVICE_ROLLBACK_URL=http://localhost:8082/elm`
3. `DB_ACCOUNT=elm_account`
4. `ACCOUNT_GRAY_MODE=canary`

说明：`ACCOUNT_SERVICE_ROLLBACK_URL` 不建议固定为 `8080`，应优先使用上一个可用地址或稳定 account-service 地址。

## 4. 场景模板

### 4.1 开发日常

1. `ACCOUNT_SERVICE_URL=http://localhost:8082/elm`
2. `ACCOUNT_SERVICE_URL_PREVIOUS=`
3. `ACCOUNT_SERVICE_ROLLBACK_URL=http://localhost:8082/elm`

### 4.2 灰度切换

1. 执行：
   - `uv run manage_account_gray.py switch --env-file .env --mode canary --target-url <target>`
2. 脚本会自动维护：
   - `ACCOUNT_SERVICE_URL_PREVIOUS=<old_url>`
   - `ACCOUNT_SERVICE_URL=<new_url>`

### 4.3 回滚

1. 优先回滚到 `ACCOUNT_SERVICE_URL_PREVIOUS`
   - `uv run rollback_account_gray.py --env-file .env`
2. 指定回滚目标（仅在明确可用时）
   - `uv run rollback_account_gray.py --env-file .env --fallback-url <known_good_url>`

## 5. 校验清单

1. Schema 校验：
   - `uv run check_account_schema.py --env-file .env`
   - 通过标准：`ACCOUNT_SCHEMA_OK=true`
2. 业务链路校验：
   - `uv run run_four_service_smoke.py --env-file .env`
   - 通过标准：`SMOKE_OK=true`
3. 灰度探针校验：
   - `uv run manage_account_gray.py status --env-file .env`
   - 通过标准：`VERIFY_OK=true`
