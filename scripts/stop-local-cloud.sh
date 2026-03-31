#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PID_DIR="$ROOT_DIR/.run"

for name in gateway-service discovery-server config-server; do
  pid_file="$PID_DIR/${name}.pid"
  if [[ ! -f "$pid_file" ]]; then
    continue
  fi

  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    echo "stopped $name ($pid)"
  else
    echo "$name already stopped"
  fi
  rm -f "$pid_file"
done
