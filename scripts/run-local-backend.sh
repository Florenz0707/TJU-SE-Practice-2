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
PROFILE="local"

export JAVA_HOME MAVEN_HOME MAVEN_REPO LOCAL_DB_BASE_DIR
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

mkdir -p "$PID_DIR" "$LOG_DIR" "$LOCAL_DB_BASE_DIR" "$MAVEN_REPO"

start_service() {
  local name="$1"
  local dir="$2"
  local port="$3"
  local log_file="$LOG_DIR/${name}.log"
  local pid_file="$PID_DIR/${name}.pid"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name already running on PID $(cat "$pid_file")"
    return 0
  fi

  pushd "$dir" >/dev/null
  nohup mvn -Dmaven.repo.local="$MAVEN_REPO" -DskipTests spring-boot:run \
    -Dspring-boot.run.profiles="$PROFILE" \
    >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  popd >/dev/null

  echo "started $name on port $port, pid $(cat "$pid_file")"
}

start_service points-service "$ROOT_DIR/elm-microservice/points-service" 8081
start_service account-service "$ROOT_DIR/elm-microservice/account-service" 8082
start_service business-service "$ROOT_DIR/elm-microservice/business-service" 8083
start_service food-service "$ROOT_DIR/elm-microservice/food-service" 8087
start_service cart-service "$ROOT_DIR/elm-microservice/cart-service" 8089
start_service order-service "$ROOT_DIR/elm-microservice/order-service" 8084
start_service address-service "$ROOT_DIR/elm-microservice/address-service" 8085
start_service user-service "$ROOT_DIR/elm-microservice/user-service" 8086
start_service elm-v2 "$ROOT_DIR/elm-v2.0" 8080

echo "backend startup commands submitted"
echo "logs: $LOG_DIR"
