#!/usr/bin/env python3
"""Phase3 account-service compensation drill."""

from __future__ import annotations

import argparse
import json
import os
import shlex
import signal
import socket
import subprocess
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests
from dotenv import load_dotenv


@dataclass
class ServiceProc:
  process: subprocess.Popen[Any]
  log_path: Path


class DrillError(RuntimeError):
  pass


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Run phase3 account-service drill.")
  parser.add_argument("--env-file", default=".env", help="Path to env file.")
  parser.add_argument(
      "--skip-start",
      action="store_true",
      help="Skip account-service startup and drill against running service.",
  )
  parser.add_argument(
      "--startup-timeout",
      type=int,
      default=120,
      help="Timeout seconds for account-service startup.",
  )
  return parser.parse_args()


def must_env(name: str) -> str:
  value = os.getenv(name)
  if value is None or value.strip() == "":
    raise DrillError(f"Missing required env: {name}")
  return value.strip()


def wait_port(host: str, port: int, timeout_seconds: int) -> None:
  deadline = time.time() + timeout_seconds
  while time.time() < deadline:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
      sock.settimeout(1.0)
      if sock.connect_ex((host, port)) == 0:
        return
    time.sleep(1)
  raise DrillError(f"Timeout waiting for port {host}:{port}")


def jdbc_url(host: str, port: str, db_name: str) -> str:
  return (
      f"jdbc:mysql://{host}:{port}/{db_name}"
      "?useUnicode=true&characterEncoding=utf8&useSSL=false"
      "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
  )


def run_service(
    repo_root: Path,
    account_url: str,
    mysql_host: str,
    mysql_port: str,
    mysql_user: str,
    mysql_password: str,
    db_account: str,
) -> ServiceProc:
  scripts_dir = repo_root / "elm-v2.0" / "scripts"
  logs_dir = scripts_dir / "logs"
  logs_dir.mkdir(parents=True, exist_ok=True)
  log_path = logs_dir / "phase3-account-service.log"
  logfile = log_path.open("w", encoding="utf-8")

  account_start_cmd = os.getenv("ACCOUNT_START_CMD", "mvn spring-boot:run")
  env = os.environ.copy()
  env["DB_URL"] = jdbc_url(mysql_host, mysql_port, db_account)
  env["DB_USERNAME"] = mysql_user
  env["DB_PASSWORD"] = mysql_password
  env["ACCOUNT_SERVICE_URL"] = account_url

  process = subprocess.Popen(
      shlex.split(account_start_cmd),
      cwd=str(repo_root / "elm-microservice" / "account-service"),
      env=env,
      stdout=logfile,
      stderr=subprocess.STDOUT,
      preexec_fn=os.setsid,
  )
  return ServiceProc(process=process, log_path=log_path)


def stop_service(service: ServiceProc | None) -> None:
  if service is None:
    return
  if service.process.poll() is not None:
    return
  try:
    os.killpg(os.getpgid(service.process.pid), signal.SIGTERM)
    time.sleep(1)
    if service.process.poll() is None:
      os.killpg(os.getpgid(service.process.pid), signal.SIGKILL)
  except ProcessLookupError:
    return


def request_json(
    method: str,
    url: str,
    *,
    token: str,
    body: dict[str, Any] | None = None,
    timeout: int = 10,
) -> dict[str, Any]:
  headers = {
      "Content-Type": "application/json",
      "X-Internal-Service-Token": token,
  }
  response = requests.request(method, url, json=body, headers=headers, timeout=timeout)
  response.raise_for_status()
  return response.json()


def assert_success(resp: dict[str, Any], name: str) -> dict[str, Any]:
  if not isinstance(resp, dict) or not resp.get("success"):
    raise DrillError(f"{name} failed: {json.dumps(resp, ensure_ascii=False)}")
  return resp


def main() -> int:
  args = parse_args()
  load_dotenv(args.env_file)

  repo_root = Path(__file__).resolve().parents[2]
  account_url = must_env("ACCOUNT_SERVICE_URL").rstrip("/")
  token = must_env("INTERNAL_SERVICE_TOKEN")
  mysql_host = must_env("MYSQL_HOST")
  mysql_port = must_env("MYSQL_PORT")
  mysql_user = must_env("MYSQL_USER")
  mysql_password = must_env("MYSQL_PASSWORD")
  db_account = must_env("DB_ACCOUNT")
  account_port = int(os.getenv("ACCOUNT_PORT", "8082"))
  drill_user_id = int(os.getenv("PHASE3_DRILL_USER_ID", "1"))
  drill_amount = os.getenv("PHASE3_DRILL_AMOUNT", "3.00")

  service_proc: ServiceProc | None = None
  try:
    if not args.skip_start:
      service_proc = run_service(
          repo_root=repo_root,
          account_url=account_url,
          mysql_host=mysql_host,
          mysql_port=mysql_port,
          mysql_user=mysql_user,
          mysql_password=mysql_password,
          db_account=db_account,
      )
      wait_port("127.0.0.1", account_port, args.startup_timeout)

    wallet_resp = request_json(
        "GET",
        f"{account_url}/api/inner/account/wallet/by-user/{drill_user_id}?createIfAbsent=true",
        token=token,
    )
    assert_success(wallet_resp, "wallet/by-user")

    suffix = uuid.uuid4().hex[:8]
    debit_request_id = f"phase3-debit-{suffix}"
    refund_request_id = f"phase3-refund-{suffix}"
    biz_id = f"PHASE3_{suffix}"

    bootstrap_refund = assert_success(
        request_json(
            "POST",
            f"{account_url}/api/inner/account/wallet/refund",
            token=token,
            body={
                "requestId": f"phase3-bootstrap-{suffix}",
                "userId": drill_user_id,
                "amount": drill_amount,
                "bizId": f"{biz_id}_BOOTSTRAP",
                "reason": "phase3 drill bootstrap balance",
            },
        ),
        "wallet/refund bootstrap",
    )

    debit_body = {
        "requestId": debit_request_id,
        "userId": drill_user_id,
        "amount": drill_amount,
        "bizId": biz_id,
        "reason": "phase3 drill debit",
    }
    debit1 = assert_success(
        request_json("POST", f"{account_url}/api/inner/account/wallet/debit", token=token, body=debit_body),
        "wallet/debit first",
    )
    debit2 = assert_success(
        request_json("POST", f"{account_url}/api/inner/account/wallet/debit", token=token, body=debit_body),
        "wallet/debit duplicate",
    )
    debit_id_1 = ((debit1.get("data") or {}).get("id"))
    debit_id_2 = ((debit2.get("data") or {}).get("id"))
    debit_idempotent = debit_id_1 is not None and debit_id_1 == debit_id_2

    refund_body = {
        "requestId": refund_request_id,
        "userId": drill_user_id,
        "amount": drill_amount,
        "bizId": biz_id,
        "reason": "phase3 drill refund",
    }
    refund1 = assert_success(
        request_json(
            "POST", f"{account_url}/api/inner/account/wallet/refund", token=token, body=refund_body
        ),
        "wallet/refund first",
    )
    refund2 = assert_success(
        request_json(
            "POST", f"{account_url}/api/inner/account/wallet/refund", token=token, body=refund_body
        ),
        "wallet/refund duplicate",
    )
    refund_id_1 = ((refund1.get("data") or {}).get("id"))
    refund_id_2 = ((refund2.get("data") or {}).get("id"))
    refund_idempotent = refund_id_1 is not None and refund_id_1 == refund_id_2

    rollback_resp = assert_success(
        request_json(
            "POST",
            f"{account_url}/api/inner/account/voucher/rollback",
            token=token,
            body={
                "requestId": f"phase3-rollback-{suffix}",
                "userId": drill_user_id,
                "voucherId": -1,
                "orderId": f"ORDER_{suffix}",
                "reason": "phase3 drill invalid voucher rollback",
            },
        ),
        "voucher/rollback invalid",
    )
    rollback_failed_as_expected = rollback_resp.get("data") is False

    unreachable_ok = False
    unreachable_error = "none"
    bad_url = "http://127.0.0.1:6553/elm/api/inner/account/wallet/by-user/1"
    try:
      requests.get(
          bad_url,
          headers={"X-Internal-Service-Token": token},
          timeout=2,
      )
      unreachable_error = "unexpectedly reachable"
    except Exception as ex:
      unreachable_ok = True
      unreachable_error = str(ex).splitlines()[0]

    result = {
        "user_id": drill_user_id,
        "amount": drill_amount,
        "bootstrap_refund_tx_id": ((bootstrap_refund.get("data") or {}).get("id")),
        "debit_request_id": debit_request_id,
        "debit_idempotent": debit_idempotent,
        "refund_request_id": refund_request_id,
        "refund_idempotent": refund_idempotent,
        "rollback_failed_as_expected": rollback_failed_as_expected,
        "unreachable_simulation_ok": unreachable_ok,
        "unreachable_error": unreachable_error,
    }
    result["DRILL_OK"] = bool(
        debit_idempotent and refund_idempotent and rollback_failed_as_expected and unreachable_ok
    )

    print("\n=== PHASE3 ACCOUNT DRILL RESULT ===")
    for key, value in result.items():
      print(f"{key}={value}")
    return 0 if result["DRILL_OK"] else 2
  finally:
    if not args.skip_start:
      stop_service(service_proc)


if __name__ == "__main__":
  raise SystemExit(main())
