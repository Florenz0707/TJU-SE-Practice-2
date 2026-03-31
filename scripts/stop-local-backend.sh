#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PID_DIR="$ROOT_DIR/.run"
BACKEND_SERVICES=(
  points-service
  account-service
  catalog-service
  catalog-service-a
  catalog-service-b
  business-service
  business-service-a
  business-service-b
  food-service
  food-service-a
  food-service-b
  cart-service
  cart-service-a
  cart-service-b
  order-service
  order-service-a
  order-service-b
  address-service
  user-service
  elm-v2
)

if [[ ! -d "$PID_DIR" ]]; then
  echo "no pid directory found"
  exit 0
fi

for name in "${BACKEND_SERVICES[@]}"; do
  pid_file="$PID_DIR/${name}.pid"
  [[ -e "$pid_file" ]] || continue
  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    echo "stopped $name ($pid)"
  else
    echo "$name already stopped"
  fi
  rm -f "$pid_file"
done
