#!/usr/bin/env python3
"""Manage account-service gray switch via env file."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Any

import requests
from dotenv import dotenv_values, load_dotenv, set_key


class SwitchError(RuntimeError):
  pass


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Manage account-service gray switch.")
  sub = parser.add_subparsers(dest="command", required=True)

  status = sub.add_parser("status", help="Print current account-service config and probe status.")
  status.add_argument("--env-file", default=".env", help="Path to env file.")
  status.add_argument("--probe-user-id", type=int, default=1, help="User id for probe API.")
  status.add_argument("--timeout", type=int, default=5, help="HTTP timeout in seconds.")
  status.add_argument("--skip-verify", action="store_true", help="Skip API probe.")

  switch = sub.add_parser("switch", help="Switch account-service URL for gray/rollback.")
  switch.add_argument("--env-file", default=".env", help="Path to env file.")
  switch.add_argument(
      "--mode",
      choices=["canary", "full", "rollback"],
      required=True,
      help="Switch mode: canary/full -> target-url, rollback -> fallback-url.",
  )
  switch.add_argument(
      "--target-url",
      default="http://localhost:8082/elm",
      help="Target account-service URL for canary/full.",
  )
  switch.add_argument(
      "--fallback-url",
      default="http://localhost:8080/elm",
      help="Fallback URL for rollback.",
  )
  switch.add_argument("--probe-user-id", type=int, default=1, help="User id for probe API.")
  switch.add_argument("--timeout", type=int, default=5, help="HTTP timeout in seconds.")
  switch.add_argument("--skip-verify", action="store_true", help="Skip API probe before switch.")
  return parser.parse_args()


def must_env(name: str) -> str:
  value = os.getenv(name)
  if value is None or value.strip() == "":
    raise SwitchError(f"Missing required env: {name}")
  return value.strip()


def probe_internal_account(url: str, token: str, user_id: int, timeout: int) -> tuple[bool, str]:
  endpoint = f"{url.rstrip('/')}/api/inner/account/wallet/by-user/{user_id}?createIfAbsent=true"
  try:
    response = requests.get(
        endpoint,
        headers={"X-Internal-Service-Token": token},
        timeout=timeout,
    )
    response.raise_for_status()
    payload: dict[str, Any] = response.json()
    if payload.get("success") is True:
      return True, "success"
    return False, json.dumps(payload, ensure_ascii=False)
  except Exception as ex:
    return False, str(ex).splitlines()[0]


def print_status(env_file: Path, probe_user_id: int, timeout: int, skip_verify: bool) -> int:
  load_dotenv(env_file)
  current = os.getenv("ACCOUNT_SERVICE_URL", "").strip()
  token = os.getenv("INTERNAL_SERVICE_TOKEN", "").strip()

  verify_ok = None
  verify_msg = "skipped"
  if not skip_verify:
    if not token:
      raise SwitchError("Missing INTERNAL_SERVICE_TOKEN for verify")
    if not current:
      raise SwitchError("Missing ACCOUNT_SERVICE_URL in env")
    verify_ok, verify_msg = probe_internal_account(current, token, probe_user_id, timeout)

  print("\n=== ACCOUNT GRAY STATUS ===")
  print(f"ENV_FILE={env_file}")
  print(f"ACCOUNT_SERVICE_URL={current}")
  print(f"VERIFY_OK={verify_ok}")
  print(f"VERIFY_MSG={verify_msg}")
  return 0 if (verify_ok is None or verify_ok) else 2


def run_switch(
    env_file: Path,
    mode: str,
    target_url: str,
    fallback_url: str,
    probe_user_id: int,
    timeout: int,
    skip_verify: bool,
) -> int:
  load_dotenv(env_file)
  token = must_env("INTERNAL_SERVICE_TOKEN")
  existing_values = dotenv_values(env_file)
  old_url = (existing_values.get("ACCOUNT_SERVICE_URL") or "").strip()
  new_url = target_url.strip() if mode in {"canary", "full"} else fallback_url.strip()
  if not new_url:
    raise SwitchError("Target URL is empty")

  verify_ok = None
  verify_msg = "skipped"
  if not skip_verify:
    verify_ok, verify_msg = probe_internal_account(new_url, token, probe_user_id, timeout)
    if not verify_ok:
      raise SwitchError(f"Probe new url failed: {verify_msg}")

  set_key(env_file, "ACCOUNT_SERVICE_URL", new_url)
  set_key(env_file, "ACCOUNT_GRAY_MODE", mode)
  if old_url:
    set_key(env_file, "ACCOUNT_SERVICE_URL_PREVIOUS", old_url)

  print("\n=== ACCOUNT GRAY SWITCH ===")
  print(f"ENV_FILE={env_file}")
  print(f"MODE={mode}")
  print(f"OLD_URL={old_url}")
  print(f"NEW_URL={new_url}")
  print(f"VERIFY_OK={verify_ok}")
  print(f"VERIFY_MSG={verify_msg}")
  print("SWITCH_OK=True")
  return 0


def main() -> int:
  args = parse_args()
  env_file = Path(args.env_file)
  if not env_file.exists():
    raise SwitchError(f"Env file not found: {env_file}")

  if args.command == "status":
    return print_status(
        env_file=env_file,
        probe_user_id=args.probe_user_id,
        timeout=args.timeout,
        skip_verify=args.skip_verify,
    )
  if args.command == "switch":
    return run_switch(
        env_file=env_file,
        mode=args.mode,
        target_url=args.target_url,
        fallback_url=args.fallback_url,
        probe_user_id=args.probe_user_id,
        timeout=args.timeout,
        skip_verify=args.skip_verify,
    )
  raise SwitchError(f"Unknown command: {args.command}")


if __name__ == "__main__":
  try:
    raise SystemExit(main())
  except SwitchError as err:
    print(f"ERROR={err}")
    raise SystemExit(2)
