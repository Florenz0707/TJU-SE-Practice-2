#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ELM_CLOUD_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_FILE="$ELM_CLOUD_DIR/config/order-service.yml"
CONFIG_SERVER_BASE="${CONFIG_SERVER_BASE:-http://localhost:8888}"
RUNTIME_ENDPOINT="${RUNTIME_ENDPOINT:-http://localhost:8080/elm/api/orders/runtime-config}"
AUTO_MODE=false

timestamp=$(date '+%Y%m%d-%H%M%S')
DEMO_MESSAGE="${DEMO_MESSAGE:-order-service defense live demo $timestamp}"
DEMO_VERSION="${DEMO_VERSION:-defense-$timestamp}"

usage() {
  cat <<EOF
用法: ./scripts/defense_config_bus_showcase.sh [选项]

选项:
  --auto                    不等待回车，自动串讲
  --message TEXT            指定演示 message
  --version TEXT            指定演示 version
  --config-server-base URL  指定 Config Server 地址
  --help                    查看帮助
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --auto)
        AUTO_MODE=true
        shift
        ;;
      --message)
        DEMO_MESSAGE=$2
        shift 2
        ;;
      --version)
        DEMO_VERSION=$2
        shift 2
        ;;
      --config-server-base)
        CONFIG_SERVER_BASE=$2
        shift 2
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

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

banner() {
  local title=$1
  echo
  echo "============================================================"
  echo "$title"
  echo "============================================================"
}

pause_step() {
  if [[ "$AUTO_MODE" == "true" || ! -t 0 ]]; then
    return
  fi
  echo
  read -r -p "按回车进入下一步..." _
}

pretty_json() {
  python3 - "$1" <<'PY'
import json
import sys

print(json.dumps(json.loads(sys.argv[1]), ensure_ascii=False, indent=2, sort_keys=True))
PY
}

print_config_lines() {
  local file=$1
  local pattern=$2
  echo "文件: ${file#$ELM_CLOUD_DIR/}"
  grep -nE "$pattern" "$file" | sed 's/^/  /'
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

trigger_busrefresh() {
  local code
  code=$(curl -sS -o /tmp/defense-busrefresh.out -w '%{http_code}' -X POST "$CONFIG_SERVER_BASE/actuator/busrefresh")
  if [[ "$code" != "204" ]]; then
    echo "busrefresh 调用失败，HTTP $code" >&2
    cat /tmp/defense-busrefresh.out >&2 || true
    exit 1
  fi
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
raise SystemExit(0 if payload.get("message") == sys.argv[2] and payload.get("version") == sys.argv[3] else 1)
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
  exit 1
}

cleanup() {
  if [[ -f "$BACKUP_FILE" ]]; then
    cp "$BACKUP_FILE" "$CONFIG_FILE"
    local original_message original_version
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
    trigger_busrefresh
    wait_for_runtime_value "$original_message" "$original_version" >/dev/null
    rm -f "$BACKUP_FILE"
  fi
}

parse_args "$@"
require_command curl
require_command python3
require_command docker

BACKUP_FILE=$(mktemp)
cp "$CONFIG_FILE" "$BACKUP_FILE"
trap cleanup EXIT

banner "Config + Bus 答辩专用一键串讲"
echo "目标: 用最少命令展示双 Config Server、discovery-first、Bus 广播和动态刷新。"
echo "当前 Config Server: $CONFIG_SERVER_BASE"
echo "当前运行时接口: $RUNTIME_ENDPOINT"

pause_step

banner "第一部分: 基础设施已经运行"
echo "下面展示当前关键容器状态。"
docker compose -f "$ELM_CLOUD_DIR/docker-compose.yml" ps rabbitmq eureka-server config-server-1 config-server-2 gateway order-service

pause_step

banner "第二部分: 客户端通过 Eureka 发现 Config Server"
print_config_lines "$ELM_CLOUD_DIR/gateway/src/main/resources/bootstrap.yml" 'discovery:|service-id: config-server|defaultZone:'
echo
print_config_lines "$ELM_CLOUD_DIR/order-service/src/main/resources/bootstrap.yml" 'discovery:|service-id: config-server|defaultZone:'

pause_step

banner "第三部分: Config Server 同时支持 native 和 git 模式"
print_config_lines "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml" 'active: \$\{CONFIG_SERVER_MODE:native\}|search-locations|on-profile: git|uri: \$\{CONFIG_GIT_URI:file:/app/config-repo\}'

pause_step

banner "第四部分: 演示刷新前的运行时配置"
baseline_payload=$(read_runtime_payload)
pretty_json "$baseline_payload"

pause_step

banner "第五部分: 修改配置并触发 Bus 广播"
echo "将 order-service 的演示配置临时改为:"
echo "  message = $DEMO_MESSAGE"
echo "  version = $DEMO_VERSION"
update_demo_config "$DEMO_MESSAGE" "$DEMO_VERSION"
trigger_busrefresh
echo "已调用 POST $CONFIG_SERVER_BASE/actuator/busrefresh"

pause_step

banner "第六部分: 展示刷新后的运行时配置"
refreshed_payload=$(wait_for_runtime_value "$DEMO_MESSAGE" "$DEMO_VERSION")
pretty_json "$refreshed_payload"

pause_step

banner "收口话术"
echo "1. 现在能看到两个 Config Server 实例和 RabbitMQ 都在运行。"
echo "2. gateway 和 order-service 已经通过 discovery-first 发现 config-server，而不是直连固定地址。"
echo "3. Config Server 默认用 native 保证演示稳定，同时也保留了 git 模式切换入口。"
echo "4. 配置修改后，通过 busrefresh 广播刷新，runtime-config 接口已经实时返回新值。"
echo "5. 脚本退出时会自动恢复原始配置，不会把演示值留在仓库里。"
