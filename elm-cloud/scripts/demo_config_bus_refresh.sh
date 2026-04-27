#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ELM_CLOUD_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_FILE="$ELM_CLOUD_DIR/config/order-service.yml"
CONFIG_SERVER_BASE="${CONFIG_SERVER_BASE:-http://localhost:8888}"
RUNTIME_ENDPOINT="${RUNTIME_ENDPOINT:-http://localhost:8080/elm/api/orders/runtime-config}"
KEEP_DEMO_VALUE=false

timestamp=$(date '+%Y%m%d-%H%M%S')
DEMO_MESSAGE="${DEMO_MESSAGE:-order-service remote config live demo $timestamp}"
DEMO_VERSION="${DEMO_VERSION:-demo-$timestamp}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

usage() {
  cat <<EOF
用法: bash scripts/demo_config_bus_refresh.sh [选项]

选项:
  --config-server-base URL   指定 Config Server 地址，默认 http://localhost:8888
  --message TEXT             指定演示 message
  --version TEXT             指定演示 version
  --keep-demo-value          演示成功后不自动恢复配置
  --help                     查看帮助
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --config-server-base)
        CONFIG_SERVER_BASE=$2
        shift 2
        ;;
      --message)
        DEMO_MESSAGE=$2
        shift 2
        ;;
      --version)
        DEMO_VERSION=$2
        shift 2
        ;;
      --keep-demo-value)
        KEEP_DEMO_VALUE=true
        shift
        ;;
      --help)
        usage
        exit 0
        ;;
      *)
        echo "未知参数: $1" >&2
        usage >&2
        exit 1
        ;;
    esac
  done
}

read_runtime_field() {
  local field=$1
  local payload
  payload=$(curl -sS "$RUNTIME_ENDPOINT")
  python3 - "$payload" "$field" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
field = sys.argv[2]
print(payload[field])
PY
}

read_runtime_payload() {
  curl -sS "$RUNTIME_ENDPOINT"
}

update_demo_config() {
  local message=$1
  local version=$2
  python3 - "$CONFIG_FILE" "$message" "$version" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
message = sys.argv[2]
version = sys.argv[3]
text = path.read_text()
text, message_count = re.subn(
    r'(^\s*message:\s*).*$',
    lambda m: f"{m.group(1)}{message}",
    text,
    count=1,
    flags=re.M,
)
text, version_count = re.subn(
    r'(^\s*version:\s*).*$',
    lambda m: f"{m.group(1)}{version}",
    text,
    count=1,
    flags=re.M,
)
if message_count != 1 or version_count != 1:
    raise SystemExit("order-service.yml 中未找到唯一的 demo.config 字段")
path.write_text(text)
PY
}

wait_for_runtime_value() {
  local expected_message=$1
  local expected_version=$2
  local attempt

  for attempt in $(seq 1 30); do
    local payload
    payload=$(read_runtime_payload)
    if python3 - "$payload" "$expected_message" "$expected_version" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
expected_message = sys.argv[2]
expected_version = sys.argv[3]
raise SystemExit(0 if payload.get("message") == expected_message and payload.get("version") == expected_version else 1)
PY
    then
      echo "$payload"
      return 0
    fi
    python3 - <<'PY'
import time
time.sleep(0.5)
PY
  done

  echo "等待运行时配置刷新超时" >&2
  return 1
}

verify_config_server_value() {
  local expected_message=$1
  local expected_version=$2
  local attempt

  for attempt in $(seq 1 20); do
    local payload
    payload=$(curl -sS "$CONFIG_SERVER_BASE/order-service/default")
    if python3 - "$payload" "$expected_message" "$expected_version" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
expected_message = sys.argv[2]
expected_version = sys.argv[3]
sources = payload.get("propertySources", [])
flattened = {}
for source in sources:
    flattened.update(source.get("source", {}))
if flattened.get("demo.config.message") != expected_message:
    raise SystemExit(f"Config Server 中 message 不匹配: {flattened.get('demo.config.message')}")
if flattened.get("demo.config.version") != expected_version:
    raise SystemExit(f"Config Server 中 version 不匹配: {flattened.get('demo.config.version')}")
print(f"[OK]   Config Server 已读取新值: message={expected_message}, version={expected_version}")
PY
    then
      return 0
    fi
    python3 - <<'PY'
import time
time.sleep(0.5)
PY
  done

  echo "等待 Config Server 读取新配置超时" >&2
  return 1
}

trigger_busrefresh() {
  local code
  code=$(curl -sS -o /tmp/config-bus-refresh.out -w '%{http_code}' -X POST "$CONFIG_SERVER_BASE/actuator/busrefresh")
  if [[ "$code" != "204" ]]; then
    echo "busrefresh 调用失败，HTTP $code" >&2
    cat /tmp/config-bus-refresh.out >&2 || true
    exit 1
  fi
  echo "[OK]   busrefresh 已触发"
}

restore_original_config() {
  if [[ -f "$BACKUP_FILE" ]]; then
    cp "$BACKUP_FILE" "$CONFIG_FILE"
  fi
}

cleanup() {
  if [[ "$KEEP_DEMO_VALUE" == "true" ]]; then
    rm -f "$BACKUP_FILE"
    return
  fi

  echo
  echo "== 恢复原始配置 =="
  restore_original_config
  original_message=$(python3 - "$BACKUP_FILE" <<'PY'
from pathlib import Path
import re
import sys

text = Path(sys.argv[1]).read_text()
match = re.search(r'^\s*message:\s*(.*)$', text, flags=re.M)
print(match.group(1) if match else "")
PY
)
  original_version=$(python3 - "$BACKUP_FILE" <<'PY'
from pathlib import Path
import re
import sys

text = Path(sys.argv[1]).read_text()
match = re.search(r'^\s*version:\s*(.*)$', text, flags=re.M)
print(match.group(1) if match else "")
PY
)
  verify_config_server_value "$original_message" "$original_version"
  trigger_busrefresh
  restored_payload=$(wait_for_runtime_value "$original_message" "$original_version")
  echo "[OK]   已恢复运行时配置: $restored_payload"
  rm -f "$BACKUP_FILE"
}

parse_args "$@"
require_command curl
require_command python3

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "未找到配置文件: $CONFIG_FILE" >&2
  exit 1
fi

BACKUP_FILE=$(mktemp)
cp "$CONFIG_FILE" "$BACKUP_FILE"
trap cleanup EXIT

echo "== Config + Bus 动态刷新自动演示 =="
echo "配置文件: $CONFIG_FILE"
echo "Config Server: $CONFIG_SERVER_BASE"
echo "Runtime 接口: $RUNTIME_ENDPOINT"

baseline_payload=$(read_runtime_payload)
echo
echo "== 演示前基线 =="
echo "$baseline_payload"

echo
echo "== 写入演示值 =="
update_demo_config "$DEMO_MESSAGE" "$DEMO_VERSION"
echo "[OK]   已修改 order-service.yml"
verify_config_server_value "$DEMO_MESSAGE" "$DEMO_VERSION"

echo
echo "== 触发总线刷新 =="
trigger_busrefresh

echo
echo "== 验证运行时配置已变化 =="
demo_payload=$(wait_for_runtime_value "$DEMO_MESSAGE" "$DEMO_VERSION")
echo "[OK]   刷新成功: $demo_payload"

if [[ "$KEEP_DEMO_VALUE" == "true" ]]; then
  trap - EXIT
  rm -f "$BACKUP_FILE"
  echo
  echo "已保留演示值，未自动恢复。"
fi