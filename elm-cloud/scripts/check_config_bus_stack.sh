#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ELM_CLOUD_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

check_http_ok() {
  local name=$1
  local url=$2
  local code
  code=$(curl -sS -o /tmp/config-bus-check.out -w '%{http_code}' "$url")
  if [[ "$code" != "200" ]]; then
    echo "[FAIL] $name 返回 HTTP $code: $url" >&2
    cat /tmp/config-bus-check.out >&2 || true
    exit 1
  fi
  echo "[OK]   $name"
}

require_contains() {
  local name=$1
  local payload=$2
  local needle=$3
  if [[ "$payload" != *"$needle"* ]]; then
    echo "[FAIL] $name 缺少关键信息: $needle" >&2
    exit 1
  fi
  echo "[OK]   $name 包含 $needle"
}

require_command curl
require_command python3
require_command docker

echo "== Config + Bus 基础健康检查 =="
echo "工作目录: $ELM_CLOUD_DIR"

check_http_ok "Eureka 首页" "http://localhost:8761"
check_http_ok "Config Server 1 健康" "http://localhost:8888/actuator/health"
check_http_ok "Config Server 2 健康" "http://localhost:8889/actuator/health"
check_http_ok "Gateway 健康" "http://localhost:8080/actuator/health"

echo
echo "== 检查 Eureka 注册表 =="
registry=$(curl -sS "http://localhost:8761/eureka/apps")
require_contains "Eureka 注册表" "$registry" "<name>CONFIG-SERVER</name>"
require_contains "Eureka 注册表" "$registry" "<name>GATEWAY</name>"
require_contains "Eureka 注册表" "$registry" "<name>ORDER-SERVICE</name>"

echo
echo "== 检查运行时配置接口 =="
runtime_payload=$(curl -sS "http://localhost:8080/elm/api/orders/runtime-config")
python3 - "$runtime_payload" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
required_keys = {"service", "message", "version"}
missing = required_keys.difference(payload)
if missing:
    raise SystemExit(f"运行时配置接口缺少字段: {sorted(missing)}")
if payload["service"] != "order-service":
    raise SystemExit(f"service 字段异常: {payload['service']}")
print(f"[OK]   runtime-config: message={payload['message']}, version={payload['version']}")
PY

echo
echo "== Docker Compose 关键服务状态 =="
docker compose -f "$ELM_CLOUD_DIR/docker-compose.yml" ps config-server-1 config-server-2 gateway order-service

echo
echo "检查完成。"