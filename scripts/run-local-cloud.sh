#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
TOOLS_DIR="$ROOT_DIR/.tools"
JAVA_HOME="$TOOLS_DIR/jdk-21"
MAVEN_HOME="$TOOLS_DIR/apache-maven-3.9.9"
MAVEN_REPO="$ROOT_DIR/.m2"
PID_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/.logs"

export JAVA_HOME MAVEN_HOME MAVEN_REPO
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
export CONFIG_REPO_PATH="$ROOT_DIR/spring-cloud-config-repo"
export CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://localhost:8888}"
export EUREKA_SERVER_URL="${EUREKA_SERVER_URL:-http://localhost:8761/eureka}"

mkdir -p "$PID_DIR" "$LOG_DIR" "$MAVEN_REPO"

start_service() {
  local name="$1"
  local dir="$2"
  local log_file="$LOG_DIR/${name}.log"
  local pid_file="$PID_DIR/${name}.pid"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name already running on PID $(cat "$pid_file")"
    return 0
  fi

  pushd "$dir" >/dev/null
  nohup mvn -Dmaven.repo.local="$MAVEN_REPO" -DskipTests spring-boot:run >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  popd >/dev/null

  echo "started $name, pid $(cat "$pid_file")"
}

start_service config-server "$ROOT_DIR/elm-microservice/config-server"
sleep 5
start_service discovery-server "$ROOT_DIR/elm-microservice/discovery-server"
sleep 5
start_service gateway-service "$ROOT_DIR/elm-microservice/gateway-service"

echo "spring cloud infrastructure startup commands submitted"
echo "logs: $LOG_DIR"
