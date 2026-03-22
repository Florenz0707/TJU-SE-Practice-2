#!/usr/bin/env python3
"""Run four-service integration smoke for elm-v2.0 orchestration."""

from __future__ import annotations

import argparse
import json
import os
import shlex
import signal
import socket
import subprocess
import sys
import time
import uuid
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import pymysql
import requests
from dotenv import load_dotenv


@dataclass
class ServiceProc:
  name: str
  process: subprocess.Popen[Any]
  log_path: Path


class SmokeError(RuntimeError):
  pass


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Run 4-service integration smoke test.")
  parser.add_argument(
      "--env-file",
      default=".env",
      help="Env file path (default: scripts/.env).",
  )
  parser.add_argument(
      "--skip-start",
      action="store_true",
      help="Do not start services; only run smoke API/db checks.",
  )
  parser.add_argument(
      "--startup-timeout",
      type=int,
      default=180,
      help="Timeout seconds for each service startup.",
  )
  return parser.parse_args()


def must_env(name: str) -> str:
  value = os.getenv(name)
  if value is None or value.strip() == "":
    raise SmokeError(f"Missing required env: {name}")
  return value.strip()


def bool_env(name: str, default: bool) -> bool:
  raw = os.getenv(name)
  if raw is None:
    return default
  return raw.strip().lower() in {"1", "true", "yes", "on"}


def jdbc_url(host: str, port: str, db_name: str) -> str:
  return (
      f"jdbc:mysql://{host}:{port}/{db_name}"
      "?useUnicode=true&characterEncoding=utf8&useSSL=false"
      "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
  )


def wait_port(host: str, port: int, timeout_seconds: int) -> None:
  deadline = time.time() + timeout_seconds
  while time.time() < deadline:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
      sock.settimeout(1.0)
      if sock.connect_ex((host, port)) == 0:
        return
    time.sleep(1)
  raise SmokeError(f"Timeout waiting for port {host}:{port}")


def run_service(
    name: str,
    workdir: Path,
    cmd: str,
    env_overrides: dict[str, str],
    logs_dir: Path,
) -> ServiceProc:
  log_path = logs_dir / f"{name}.log"
  env = os.environ.copy()
  env.update(env_overrides)
  logfile = log_path.open("w", encoding="utf-8")
  process = subprocess.Popen(
      shlex.split(cmd),
      cwd=str(workdir),
      env=env,
      stdout=logfile,
      stderr=subprocess.STDOUT,
      preexec_fn=os.setsid,
  )
  return ServiceProc(name=name, process=process, log_path=log_path)


def stop_services(services: list[ServiceProc]) -> None:
  for svc in reversed(services):
    if svc.process.poll() is not None:
      continue
    try:
      os.killpg(os.getpgid(svc.process.pid), signal.SIGTERM)
    except ProcessLookupError:
      continue
  time.sleep(1)
  for svc in reversed(services):
    if svc.process.poll() is None:
      try:
        os.killpg(os.getpgid(svc.process.pid), signal.SIGKILL)
      except ProcessLookupError:
        pass


def request_json(
    method: str,
    url: str,
    *,
    token: str | None = None,
    body: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout: int = 15,
) -> dict[str, Any]:
  req_headers = {"Content-Type": "application/json"}
  if token:
    req_headers["Authorization"] = f"Bearer {token}"
  if headers:
    req_headers.update(headers)
  response = requests.request(method, url, json=body, headers=req_headers, timeout=timeout)
  response.raise_for_status()
  return response.json()


def assert_success(resp: dict[str, Any], name: str) -> dict[str, Any]:
  if not isinstance(resp, dict) or not resp.get("success"):
    raise SmokeError(f"{name} failed: {json.dumps(resp, ensure_ascii=False)}")
  return resp


def open_mysql(host: str, port: int, user: str, password: str, database: str):
  return pymysql.connect(
      host=host,
      port=port,
      user=user,
      password=password,
      database=database,
      charset="utf8mb4",
      autocommit=True,
      cursorclass=pymysql.cursors.DictCursor,
  )


def ensure_business_and_food(
    conn,
    *,
    business_id: int,
    food_id: int,
    owner_id: int,
    tag: str,
) -> tuple[int, int]:
  now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
  with conn.cursor() as cur:
    cur.execute(
        """
        INSERT INTO business
          (id, create_time, update_time, is_deleted, business_name, user_id, business_address,
           business_explain, business_img, order_type_id, start_price, delivery_price,
           remarks, open_time, close_time)
        VALUES
          (%s, %s, %s, 0, %s, %s, %s, %s, '', 1, 1.00, 0.00, 'smoke-seed', '00:00:00', '23:59:59')
        ON DUPLICATE KEY UPDATE
          is_deleted=0,
          update_time=VALUES(update_time),
          business_name=VALUES(business_name),
          user_id=VALUES(user_id),
          business_address=VALUES(business_address),
          business_explain=VALUES(business_explain),
          start_price=VALUES(start_price),
          delivery_price=VALUES(delivery_price),
          open_time=VALUES(open_time),
          close_time=VALUES(close_time)
        """,
        (
            business_id,
            now,
            now,
            f"Smoke Business {tag}",
            owner_id,
            "Smoke Road 1",
            "smoke business",
        ),
    )
    cur.execute(
        """
        INSERT INTO food
          (id, create_time, update_time, is_deleted, food_name, food_explain, food_img,
           food_price, business_id, stock, remarks)
        VALUES
          (%s, %s, %s, 0, %s, %s, '', 12.00, %s, 999, 'smoke-seed')
        ON DUPLICATE KEY UPDATE
          is_deleted=0,
          update_time=VALUES(update_time),
          food_name=VALUES(food_name),
          food_explain=VALUES(food_explain),
          food_price=VALUES(food_price),
          business_id=VALUES(business_id),
          stock=VALUES(stock)
        """,
        (food_id, now, now, f"Smoke Food {tag}", "smoke food", business_id),
    )
  return business_id, food_id


def ensure_wallet(conn, owner_id: int, amount: str) -> None:
  now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
  with conn.cursor() as cur:
    cur.execute("SELECT id FROM wallet WHERE owner_id=%s", (owner_id,))
    row = cur.fetchone()
    if row is None:
      cur.execute(
          """
          INSERT INTO wallet
            (create_time, update_time, is_deleted, balance, voucher, owner_id, credit_limit,
             last_withdrawal_at, version)
          VALUES
            (%s, %s, 0, %s, 0.00, %s, 0.00, NULL, 0)
          """,
          (now, now, amount, owner_id),
      )
    else:
      cur.execute(
          "UPDATE wallet SET balance=%s, is_deleted=0, update_time=%s WHERE owner_id=%s",
          (amount, now, owner_id),
      )


def latest_outbox_id(conn) -> int:
  with conn.cursor() as cur:
    cur.execute("SELECT COALESCE(MAX(id), 0) AS max_id FROM integration_outbox_event")
    row = cur.fetchone()
    return int(row["max_id"])


def wait_outbox_sent(conn, *, min_id: int, timeout_seconds: int = 45) -> dict[str, Any]:
  deadline = time.time() + timeout_seconds
  while time.time() < deadline:
    with conn.cursor() as cur:
      cur.execute(
          """
          SELECT id, event_type, status, retry_count, last_error, processed_at
          FROM integration_outbox_event
          WHERE id > %s AND event_type='POINTS_ORDER_SUCCESS'
          ORDER BY id DESC
          LIMIT 1
          """,
          (min_id,),
      )
      row = cur.fetchone()
      if row and row["status"] == "SENT":
        return row
    time.sleep(2)
  raise SmokeError("Timeout waiting outbox POINTS_ORDER_SUCCESS -> SENT")


def first_data_id(resp: dict[str, Any], fallback_field: str = "id") -> int:
  data = resp.get("data")
  if isinstance(data, dict) and fallback_field in data:
    return int(data[fallback_field])
  raise SmokeError(f"Response data missing {fallback_field}: {json.dumps(resp, ensure_ascii=False)}")


def build_env(repo_root: Path) -> dict[str, Any]:
  mysql_host = must_env("MYSQL_HOST")
  mysql_port = must_env("MYSQL_PORT")
  mysql_user = must_env("MYSQL_USER")
  mysql_password = must_env("MYSQL_PASSWORD")

  db_main = must_env("DB_MAIN")
  db_points = must_env("DB_POINTS")
  db_account = must_env("DB_ACCOUNT")
  db_catalog = must_env("DB_CATALOG")
  db_order = must_env("DB_ORDER")

  points_url = must_env("POINTS_SERVICE_URL")
  account_url = must_env("ACCOUNT_SERVICE_URL")
  catalog_url = must_env("CATALOG_SERVICE_URL")
  order_url = must_env("ORDER_SERVICE_URL")

  return {
      "repo_root": repo_root,
      "scripts_dir": repo_root / "elm-v2.0" / "scripts",
      "logs_dir": repo_root / "elm-v2.0" / "scripts" / "logs",
      "mysql": {
          "host": mysql_host,
          "port": int(mysql_port),
          "user": mysql_user,
          "password": mysql_password,
      },
      "db": {
          "main": db_main,
          "points": db_points,
          "account": db_account,
          "catalog": db_catalog,
          "order": db_order,
      },
      "url": {
          "points": points_url,
          "account": account_url,
          "catalog": catalog_url,
          "order": order_url,
          "gateway": must_env("GATEWAY_URL"),
      },
      "internal_token": must_env("INTERNAL_SERVICE_TOKEN"),
      "wallet_topup": must_env("SMOKE_WALLET_BALANCE"),
      "keep_services": bool_env("KEEP_SERVICES_RUNNING", False),
      "cmd": {
          "points": os.getenv("POINTS_START_CMD", "mvn spring-boot:run"),
          "account": os.getenv("ACCOUNT_START_CMD", "mvn spring-boot:run"),
          "catalog": os.getenv("CATALOG_START_CMD", "mvn spring-boot:run"),
          "order": os.getenv("ORDER_START_CMD", "mvn spring-boot:run"),
          "gateway": os.getenv("GATEWAY_START_CMD", "mvn spring-boot:run"),
      },
      "service_dir": {
          "points": repo_root / "elm-microservice" / "points-service",
          "account": repo_root / "elm-microservice" / "account-service",
          "catalog": repo_root / "elm-microservice" / "catalog-service",
          "order": repo_root / "elm-microservice" / "order-service",
          "gateway": repo_root / "elm-v2.0",
      },
      "service_port": {
          "points": int(os.getenv("POINTS_PORT", "8081")),
          "account": int(os.getenv("ACCOUNT_PORT", "8082")),
          "catalog": int(os.getenv("CATALOG_PORT", "8083")),
          "order": int(os.getenv("ORDER_PORT", "8084")),
          "gateway": int(os.getenv("GATEWAY_PORT", "8080")),
      },
  }


def main() -> int:
  args = parse_args()
  script_path = Path(__file__).resolve()
  scripts_dir = script_path.parent
  repo_root = scripts_dir.parent.parent
  env_file = Path(args.env_file)
  if not env_file.is_absolute():
    env_file = scripts_dir / env_file
  if not env_file.exists():
    raise SmokeError(f"Env file not found: {env_file}")
  load_dotenv(env_file)

  cfg = build_env(repo_root)
  cfg["logs_dir"].mkdir(parents=True, exist_ok=True)

  services: list[ServiceProc] = []
  main_conn = None
  account_conn = None
  catalog_conn = None

  try:
    mysql_cfg = cfg["mysql"]
    db_cfg = cfg["db"]

    if not args.skip_start:
      points_env = {
          "DB_URL": jdbc_url(mysql_cfg["host"], str(mysql_cfg["port"]), db_cfg["points"]),
          "DB_USERNAME": mysql_cfg["user"],
          "DB_PASSWORD": mysql_cfg["password"],
      }
      account_env = {
          "DB_URL": jdbc_url(mysql_cfg["host"], str(mysql_cfg["port"]), db_cfg["account"]),
          "DB_USERNAME": mysql_cfg["user"],
          "DB_PASSWORD": mysql_cfg["password"],
      }
      catalog_env = {
          "DB_URL": jdbc_url(mysql_cfg["host"], str(mysql_cfg["port"]), db_cfg["catalog"]),
          "DB_USERNAME": mysql_cfg["user"],
          "DB_PASSWORD": mysql_cfg["password"],
      }
      order_env = {
          "DB_URL": jdbc_url(mysql_cfg["host"], str(mysql_cfg["port"]), db_cfg["order"]),
          "DB_USERNAME": mysql_cfg["user"],
          "DB_PASSWORD": mysql_cfg["password"],
      }
      gateway_env = {
          "DB_URL": jdbc_url(mysql_cfg["host"], str(mysql_cfg["port"]), db_cfg["main"]),
          "DB_USERNAME": mysql_cfg["user"],
          "DB_PASSWORD": mysql_cfg["password"],
          "POINTS_SERVICE_URL": cfg["url"]["points"],
          "ACCOUNT_SERVICE_URL": cfg["url"]["account"],
          "CATALOG_SERVICE_URL": cfg["url"]["catalog"],
          "ORDER_SERVICE_URL": cfg["url"]["order"],
          "INTERNAL_SERVICE_TOKEN": cfg["internal_token"],
      }

      for svc_name, svc_env in [
          ("points", points_env),
          ("account", account_env),
          ("catalog", catalog_env),
          ("order", order_env),
          ("gateway", gateway_env),
      ]:
        service = run_service(
            svc_name,
            cfg["service_dir"][svc_name],
            cfg["cmd"][svc_name],
            svc_env,
            cfg["logs_dir"],
        )
        services.append(service)
        wait_port("127.0.0.1", cfg["service_port"][svc_name], args.startup_timeout)

    gateway = cfg["url"]["gateway"].rstrip("/")
    main_conn = open_mysql(**mysql_cfg, database=db_cfg["main"])
    account_conn = open_mysql(**mysql_cfg, database=db_cfg["account"])
    catalog_conn = open_mysql(**mysql_cfg, database=db_cfg["catalog"])

    smoke_tag = str(int(time.time()))
    smoke_username = f"smoke_user_{smoke_tag}"

    create_user_resp = request_json(
        "POST",
        f"{gateway}/api/persons",
        body={"username": smoke_username},
    )
    assert_success(create_user_resp, "create smoke user")
    user_id = first_data_id(create_user_resp)

    auth_resp = request_json(
        "POST",
        f"{gateway}/api/auth",
        body={"username": smoke_username, "password": "password", "rememberMe": True},
    )
    token = auth_resp.get("id_token")
    if not token:
      raise SmokeError(f"auth failed: {json.dumps(auth_resp, ensure_ascii=False)}")

    ensure_wallet(account_conn, user_id, cfg["wallet_topup"])

    seed_base = int(time.time())
    business_id = seed_base
    food_id = seed_base + 1

    ensure_business_and_food(
        main_conn,
        business_id=business_id,
        food_id=food_id,
        owner_id=user_id,
        tag=smoke_tag,
    )
    ensure_business_and_food(
        catalog_conn,
        business_id=business_id,
        food_id=food_id,
        owner_id=user_id,
        tag=smoke_tag,
    )

    address_resp = request_json(
        "POST",
        f"{gateway}/api/addresses",
        token=token,
        body={
            "contactName": "Smoke",
            "contactSex": 1,
            "contactTel": "18800000000",
            "address": "Smoke Address",
        },
    )
    assert_success(address_resp, "create address")
    address_id = first_data_id(address_resp)

    outbox_base_id = latest_outbox_id(main_conn)

    cart_resp_1 = request_json(
        "POST",
        f"{gateway}/api/carts",
        token=token,
        body={"food": {"id": food_id}, "quantity": 1},
    )
    assert_success(cart_resp_1, "add cart 1")

    order_req_1 = f"smoke-order-{uuid.uuid4()}"
    order_resp_1 = request_json(
        "POST",
        f"{gateway}/api/orders",
        token=token,
        headers={"X-Request-Id": order_req_1},
        body={
            "business": {"id": business_id},
            "deliveryAddress": {"id": address_id},
            "walletPaid": "12.00",
        },
    )
    assert_success(order_resp_1, "create order 1")
    order_id_1 = first_data_id(order_resp_1)

    cancel_resp = request_json("POST", f"{gateway}/api/orders/{order_id_1}/cancel", token=token)
    assert_success(cancel_resp, "cancel order 1")
    cancel_state = cancel_resp.get("data", {}).get("orderState")

    cart_resp_2 = request_json(
        "POST",
        f"{gateway}/api/carts",
        token=token,
        body={"food": {"id": food_id}, "quantity": 1},
    )
    assert_success(cart_resp_2, "add cart 2")

    order_req_2 = f"smoke-order-{uuid.uuid4()}"
    order_resp_2 = request_json(
        "POST",
        f"{gateway}/api/orders",
        token=token,
        headers={"X-Request-Id": order_req_2},
        body={
            "business": {"id": business_id},
            "deliveryAddress": {"id": address_id},
            "walletPaid": "12.00",
        },
    )
    assert_success(order_resp_2, "create order 2")
    order_id_2 = first_data_id(order_resp_2)

    complete_resp = request_json(
        "PATCH",
        f"{gateway}/api/orders",
        token=token,
        body={"id": order_id_2, "orderState": 4},
    )
    assert_success(complete_resp, "complete order 2")
    complete_state = complete_resp.get("data", {}).get("orderState")

    outbox_row = wait_outbox_sent(main_conn, min_id=outbox_base_id)

    print("\n=== SMOKE RESULT ===")
    print(f"SMOKE_USER={smoke_username}")
    print(f"USER_ID={user_id}")
    print(f"BUSINESS_ID={business_id}")
    print(f"FOOD_ID={food_id}")
    print(f"ADDRESS_ID={address_id}")
    print(f"ORDER1_ID={order_id_1}")
    print(f"ORDER2_ID={order_id_2}")
    print(f"CANCEL1_STATE={cancel_state}")
    print(f"COMPLETE2_STATE={complete_state}")
    print(f"OUTBOX_EVENT_ID={outbox_row['id']}")
    print(f"OUTBOX_EVENT_TYPE={outbox_row['event_type']}")
    print(f"OUTBOX_STATUS={outbox_row['status']}")
    print(f"OUTBOX_RETRY={outbox_row['retry_count']}")
    print(f"OUTBOX_LAST_ERROR={outbox_row['last_error']}")

    if cancel_state != 0:
      raise SmokeError(f"cancel state expected 0, got {cancel_state}")
    if complete_state != 4:
      raise SmokeError(f"complete state expected 4, got {complete_state}")

    print("SMOKE_OK=true")
    return 0

  finally:
    if main_conn:
      main_conn.close()
    if account_conn:
      account_conn.close()
    if catalog_conn:
      catalog_conn.close()

    keep_running = bool_env("KEEP_SERVICES_RUNNING", False)
    if services and not keep_running:
      stop_services(services)
      print("Stopped started services.")
    elif services:
      print("KEEP_SERVICES_RUNNING=true, services are left running.")


if __name__ == "__main__":
  try:
    raise SystemExit(main())
  except SmokeError as exc:
    print(f"SMOKE_OK=false\nERROR={exc}", file=sys.stderr)
    raise SystemExit(2)
