#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ELM_CLOUD_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
TASK_DIR=$(cd -- "$ELM_CLOUD_DIR/.." && pwd)/task
STRICT_MANUAL=false

pass_count=0
fail_count=0
manual_count=0
warn_count=0

SERVICE_MODULES=(gateway order-service user-service merchant-service product-service cart-service address-service points-service wallet-service)
BUS_SERVICE_MODULES=(config-server gateway order-service user-service merchant-service product-service cart-service address-service points-service wallet-service)
CONFIG_FILES=(application.yml gateway.yml order-service.yml user-service.yml merchant-service.properties product-service.properties cart-service.properties address-service.properties points-service.properties wallet-service.properties)
CONTROLLER_FILES=(
  gateway/src/main/java/cn/edu/tju/elm/cloud/gateway/FallbackController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/PublicVoucherController.java
  product-service/src/main/java/cn/edu/tju/product/controller/FoodController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/PrivateVoucherController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/AccountInnerController.java
  product-service/src/main/java/cn/edu/tju/product/controller/ProductInnerController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/WalletController.java
  address-service/src/main/java/cn/edu/tju/address/controller/AddressInnerController.java
  cart-service/src/main/java/cn/edu/tju/cart/controller/CartController.java
  cart-service/src/main/java/cn/edu/tju/cart/controller/CartInnerController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/WalletInnerController.java
  wallet-service/src/main/java/cn/edu/tju/wallet/controller/TransactionController.java
  address-service/src/main/java/cn/edu/tju/address/controller/AddressController.java
  user-service/src/main/java/cn/edu/tju/core/security/controller/AuthenticationRestController.java
  user-service/src/main/java/cn/edu/tju/core/security/controller/UserRestController.java
  points-service/src/main/java/cn/edu/tju/points/controller/PointsInnerController.java
  points-service/src/main/java/cn/edu/tju/points/controller/PointsController.java
  points-service/src/main/java/cn/edu/tju/points/controller/PointsAdminController.java
  merchant-service/src/main/java/cn/edu/tju/merchant/controller/MerchantInnerController.java
  merchant-service/src/main/java/cn/edu/tju/merchant/controller/MerchantApplicationController.java
  merchant-service/src/main/java/cn/edu/tju/merchant/controller/BusinessApplicationController.java
  merchant-service/src/main/java/cn/edu/tju/merchant/controller/BusinessController.java
  order-service/src/main/java/cn/edu/tju/order/controller/OrderRestController.java
  order-service/src/main/java/cn/edu/tju/order/controller/OrderInnerController.java
  order-service/src/main/java/cn/edu/tju/order/controller/ReviewRestController.java
  order-service/src/main/java/cn/edu/tju/order/controller/RuntimeConfigController.java
)

usage() {
  cat <<EOF
用法: ./scripts/verify_task_requirements.sh [选项]

选项:
  --strict-manual    只要存在 MANUAL 项就返回非 0
  --help             查看帮助
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --strict-manual)
        STRICT_MANUAL=true
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

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

section() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

record_pass() {
  pass_count=$((pass_count + 1))
  echo "[PASS] $1"
}

record_fail() {
  fail_count=$((fail_count + 1))
  echo "[FAIL] $1"
}

record_manual() {
  manual_count=$((manual_count + 1))
  echo "[MANUAL] $1"
}

record_warn() {
  warn_count=$((warn_count + 1))
  echo "[WARN] $1"
}

assert_file_exists() {
  local description=$1
  local path=$2
  if [[ -f "$path" ]]; then
    record_pass "$description"
  else
    record_fail "$description 缺少文件: $path"
  fi
}

assert_grep() {
  local description=$1
  local pattern=$2
  local path=$3
  if grep -qE "$pattern" "$path"; then
    record_pass "$description"
  else
    record_fail "$description 未命中: ${path#$ELM_CLOUD_DIR/}"
  fi
}

assert_http_code() {
  local description=$1
  local url=$2
  local expected_code=$3
  local code
  code=$(curl -sS -o /tmp/verify-task.out -w '%{http_code}' "$url") || code="000"
  if [[ "$code" == "$expected_code" ]]; then
    record_pass "$description"
  else
    record_fail "$description 返回 HTTP $code，期望 $expected_code ($url)"
  fi
}

assert_registry_contains() {
  local description=$1
  local payload=$2
  local needle=$3
  if [[ "$payload" == *"$needle"* ]]; then
    record_pass "$description"
  else
    record_fail "$description 缺少注册信息: $needle"
  fi
}

assert_registry_count_at_least() {
  local description=$1
  local payload=$2
  local needle=$3
  local minimum=$4
  local actual
  actual=$(python3 - "$payload" "$needle" <<'PY'
import sys
print(sys.argv[1].count(sys.argv[2]))
PY
)
  if (( actual >= minimum )); then
    record_pass "$description"
  else
    record_fail "$description 数量不足: 实际 $actual, 期望至少 $minimum"
  fi
}

parse_args "$@"
require_command curl
require_command docker
require_command python3
require_command grep

section "按 task/ 逐项验收 Config + Bus 要求"
echo "task 目录: $TASK_DIR"

section "一、配置中心基础（对应 配置中心.txt）"
assert_file_exists "Config Server 启动类存在" "$ELM_CLOUD_DIR/config-server/src/main/java/cn/edu/tju/configserver/ConfigServerApplication.java"
assert_grep "Config Server 已开启 @EnableConfigServer" '@EnableConfigServer' "$ELM_CLOUD_DIR/config-server/src/main/java/cn/edu/tju/configserver/ConfigServerApplication.java"
assert_grep "Config Server 支持 native 模式" 'active: \$\{CONFIG_SERVER_MODE:native\}' "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml"
assert_grep "Config Server 支持 git profile" 'on-profile: git' "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml"
assert_grep "Config Server 预留 CONFIG_GIT_URI" 'uri: \$\{CONFIG_GIT_URI:file:/app/config-repo\}' "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml"
for config_file in "${CONFIG_FILES[@]}"; do
  assert_file_exists "集中配置文件存在: $config_file" "$ELM_CLOUD_DIR/config/$config_file"
done

section "二、Config 集中配置管理集群（对应 Config集中配置管理集群.txt）"
assert_grep "docker-compose 已声明 config-server-1" '^  config-server-1:' "$ELM_CLOUD_DIR/docker-compose.yml"
assert_grep "docker-compose 已声明 config-server-2" '^  config-server-2:' "$ELM_CLOUD_DIR/docker-compose.yml"
assert_grep "docker-compose 允许切换 CONFIG_SERVER_MODE" 'CONFIG_SERVER_MODE=\$\{CONFIG_SERVER_MODE:-native\}' "$ELM_CLOUD_DIR/docker-compose.yml"
assert_grep "docker-compose 提供 CONFIG_GIT_URI" 'CONFIG_GIT_URI=\$\{CONFIG_GIT_URI:-file:/app/config-repo\}' "$ELM_CLOUD_DIR/docker-compose.yml"
for module in "${SERVICE_MODULES[@]}"; do
  assert_file_exists "$module 存在 bootstrap.yml" "$ELM_CLOUD_DIR/$module/src/main/resources/bootstrap.yml"
  assert_grep "$module 已开启 discovery-first" 'enabled: true' "$ELM_CLOUD_DIR/$module/src/main/resources/bootstrap.yml"
  assert_grep "$module 通过 service-id 发现 config-server" 'service-id: config-server' "$ELM_CLOUD_DIR/$module/src/main/resources/bootstrap.yml"
done
assert_http_code "Config Server 1 健康接口可用" 'http://localhost:8888/actuator/health' '200'
assert_http_code "Config Server 2 健康接口可用" 'http://localhost:8889/actuator/health' '200'
registry=$(curl -sS 'http://localhost:8761/eureka/apps')
assert_registry_contains "Eureka 已注册 CONFIG-SERVER" "$registry" '<name>CONFIG-SERVER</name>'
assert_registry_count_at_least "Eureka 中至少有两个 CONFIG-SERVER 实例" "$registry" '<app>CONFIG-SERVER</app>' 2

section "三、Bus 配置刷新（对应 Bus配置刷新.txt）"
assert_grep "docker-compose 已部署 RabbitMQ 管理镜像" 'image: rabbitmq:3-management' "$ELM_CLOUD_DIR/docker-compose.yml"
for module in "${BUS_SERVICE_MODULES[@]}"; do
  assert_grep "$module 已加入 Spring Cloud Bus" 'spring-cloud-starter-bus-amqp' "$ELM_CLOUD_DIR/$module/pom.xml"
  assert_grep "$module 已加入 Actuator" 'spring-boot-starter-actuator' "$ELM_CLOUD_DIR/$module/pom.xml"
done
for module in "${SERVICE_MODULES[@]}"; do
  assert_grep "$module 已加入 Spring Cloud Config" 'spring-cloud-starter-config' "$ELM_CLOUD_DIR/$module/pom.xml"
  assert_grep "$module 已加入 bootstrap 支持" 'spring-cloud-starter-bootstrap' "$ELM_CLOUD_DIR/$module/pom.xml"
done
assert_grep "Config Server 已暴露 bus-refresh" 'bus-refresh' "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml"
assert_grep "Config Server 已暴露 bus-env" 'bus-env' "$ELM_CLOUD_DIR/config-server/src/main/resources/application.yml"
bus_code=$(curl -sS -o /tmp/verify-busrefresh.out -w '%{http_code}' -X POST 'http://localhost:8888/actuator/busrefresh')
if [[ "$bus_code" == '204' ]]; then
  record_pass "运行态 busrefresh 端点可调用"
else
  record_fail "运行态 busrefresh 端点调用失败，HTTP $bus_code"
fi

section "四、动态刷新配置（对应 动态刷新配置.txt）"
assert_grep "order-service 存在 @RefreshScope 演示 Bean" '@RefreshScope' "$ELM_CLOUD_DIR/order-service/src/main/java/cn/edu/tju/order/config/RefreshableDemoProperties.java"
assert_grep "order-service 演示 Bean 绑定 demo.config" '@ConfigurationProperties\(prefix = "demo.config"\)' "$ELM_CLOUD_DIR/order-service/src/main/java/cn/edu/tju/order/config/RefreshableDemoProperties.java"
assert_grep "order-service 暴露 runtime-config 演示接口" '/api/orders/runtime-config' "$ELM_CLOUD_DIR/order-service/src/main/java/cn/edu/tju/order/controller/RuntimeConfigController.java"
for controller_file in "${CONTROLLER_FILES[@]}"; do
  assert_grep "${controller_file##*/} 已添加 @RefreshScope" '@RefreshScope' "$ELM_CLOUD_DIR/$controller_file"
done
assert_http_code "Gateway 健康接口可用" 'http://localhost:8080/actuator/health' '200'
assert_http_code "运行时配置接口可用" 'http://localhost:8080/elm/api/orders/runtime-config' '200'
if (cd "$ELM_CLOUD_DIR" && bash ./scripts/demo_config_bus_refresh.sh >/tmp/verify-demo-script.out 2>&1); then
  record_pass "动态刷新端到端演示脚本执行成功"
else
  record_fail "动态刷新端到端演示脚本执行失败，详见 /tmp/verify-demo-script.out"
fi

section "五、与 task 文本不完全等价的人工说明"
record_manual "task 文档要求的是远程 GIT 仓库；当前项目默认运行态是 native，脚本只能自动证明“代码支持 git 模式”，不能替你证明“远程仓库账号、网络和权限都正确”。"

section "验收汇总"
echo "PASS   : $pass_count"
echo "WARN   : $warn_count"
echo "MANUAL : $manual_count"
echo "FAIL   : $fail_count"

if (( fail_count > 0 )); then
  exit 1
fi

if [[ "$STRICT_MANUAL" == "true" && $manual_count -gt 0 ]]; then
  exit 2
fi
