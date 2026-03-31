#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
TOOLS_DIR="$ROOT_DIR/.tools"
JAVA_HOME="$TOOLS_DIR/jdk-21"
MAVEN_HOME="$TOOLS_DIR/apache-maven-3.9.9"
MAVEN_REPO="$ROOT_DIR/.m2"
PID_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/.logs"
LOCAL_DB_BASE_DIR="$ROOT_DIR/.localdb"
PROFILE="local,cloud"

export JAVA_HOME MAVEN_HOME MAVEN_REPO LOCAL_DB_BASE_DIR
export CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://localhost:8888}"
export EUREKA_SERVER_URL="${EUREKA_SERVER_URL:-http://localhost:8761/eureka}"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

mkdir -p "$PID_DIR" "$LOG_DIR" "$LOCAL_DB_BASE_DIR" "$MAVEN_REPO"

start_service() {
  local name="$1"
  local dir="$2"
  local port="$3"
  local extra_args="${4:-}"
  local log_file="$LOG_DIR/${name}.log"
  local pid_file="$PID_DIR/${name}.pid"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name already running on PID $(cat "$pid_file")"
    return 0
  fi

  pushd "$dir" >/dev/null
  nohup mvn -Dmaven.repo.local="$MAVEN_REPO" -DskipTests spring-boot:run \
    -Dspring-boot.run.profiles="$PROFILE" \
    -Dspring-boot.run.arguments="--server.port=${port} ${extra_args}" \
    >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  popd >/dev/null

  echo "started $name on port $port, pid $(cat "$pid_file")"
}

start_service points-service "$ROOT_DIR/elm-microservice/points-service" 8081 "--eureka.instance.instance-id=points-service-8081"
start_service account-service "$ROOT_DIR/elm-microservice/account-service" 8082 "--eureka.instance.instance-id=account-service-8082"
sleep 5
start_service business-service-a "$ROOT_DIR/elm-microservice/business-service" 8083 "--eureka.instance.instance-id=business-service-8083"
sleep 8
start_service business-service-b "$ROOT_DIR/elm-microservice/business-service" 8183 "--eureka.instance.instance-id=business-service-8183"
sleep 8
start_service food-service-a "$ROOT_DIR/elm-microservice/food-service" 8087 "--eureka.instance.instance-id=food-service-8087"
sleep 8
start_service food-service-b "$ROOT_DIR/elm-microservice/food-service" 8187 "--eureka.instance.instance-id=food-service-8187"
sleep 5
start_service cart-service-a "$ROOT_DIR/elm-microservice/cart-service" 8089 "--eureka.instance.instance-id=cart-service-8089"
start_service cart-service-b "$ROOT_DIR/elm-microservice/cart-service" 8189 "--eureka.instance.instance-id=cart-service-8189"
start_service order-service-a "$ROOT_DIR/elm-microservice/order-service" 8084 "--eureka.instance.instance-id=order-service-8084"
start_service order-service-b "$ROOT_DIR/elm-microservice/order-service" 8184 "--eureka.instance.instance-id=order-service-8184"
start_service address-service "$ROOT_DIR/elm-microservice/address-service" 8085 "--eureka.instance.instance-id=address-service-8085"
start_service user-service "$ROOT_DIR/elm-microservice/user-service" 8086 "--eureka.instance.instance-id=user-service-8086"
start_service elm-v2 "$ROOT_DIR/elm-v2.0" 8080 "--eureka.instance.instance-id=elm-8080"

echo "cloud-enabled backend startup commands submitted"
echo "logs: $LOG_DIR"
