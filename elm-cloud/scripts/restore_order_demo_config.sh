#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ELM_CLOUD_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_FILE="$ELM_CLOUD_DIR/config/order-service.yml"
CONFIG_SERVER_BASE="${CONFIG_SERVER_BASE:-http://localhost:8888}"
RUNTIME_ENDPOINT="${RUNTIME_ENDPOINT:-http://localhost:8080/elm/api/orders/runtime-config}"
DEFAULT_MESSAGE="${DEFAULT_MESSAGE:-order-service remote config ready}"
DEFAULT_VERSION="${DEFAULT_VERSION:-v1}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
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

verify_config_server_value() {
  local expected_message=$1
  local expected_version=$2
  local payload
  payload=$(curl -sS "$CONFIG_SERVER_BASE/order-service/default")
  python3 - "$payload" "$expected_message" "$expected_version" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
expected_message = sys.argv[2]
expected_version = sys.argv[3]
sources = payload.get("propertySources", [])
flattened = {}
for source in sources:
    flattened.update(source.get("source", {}))
if flattened.get("demo.config.message") != expected_message or flattened.get("demo.config.version") != expected_version:
    raise SystemExit("Config Server 尚未读到恢复值")
print(f"[OK]   Config Server 恢复为 message={expected_message}, version={expected_version}")
PY
}

trigger_busrefresh() {
  local code
  code=$(curl -sS -o /tmp/config-bus-restore.out -w '%{http_code}' -X POST "$CONFIG_SERVER_BASE/actuator/busrefresh")
  if [[ "$code" != "204" ]]; then
    echo "busrefresh 调用失败，HTTP $code" >&2
    cat /tmp/config-bus-restore.out >&2 || true
    exit 1
  fi
  echo "[OK]   busrefresh 已触发"
}

wait_for_runtime_value() {
  local expected_message=$1
  local expected_version=$2
  local attempt

  for attempt in $(seq 1 30); do
    local payload
    payload=$(curl -sS "$RUNTIME_ENDPOINT")
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

  echo "等待恢复后的运行时配置超时" >&2
  return 1
}

require_command curl
require_command python3

echo "== 恢复 order-service 演示配置 =="
update_demo_config "$DEFAULT_MESSAGE" "$DEFAULT_VERSION"
verify_config_server_value "$DEFAULT_MESSAGE" "$DEFAULT_VERSION"
trigger_busrefresh
restored_payload=$(wait_for_runtime_value "$DEFAULT_MESSAGE" "$DEFAULT_VERSION")
echo "[OK]   已恢复运行时配置: $restored_payload"