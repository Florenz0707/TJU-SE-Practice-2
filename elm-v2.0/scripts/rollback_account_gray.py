#!/usr/bin/env python3
"""Rollback account-service gray switch to fallback URL."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

from dotenv import dotenv_values, load_dotenv, set_key

from manage_account_gray import SwitchError, probe_internal_account


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Rollback account-service gray switch.")
  parser.add_argument("--env-file", default=".env", help="Path to env file.")
  parser.add_argument(
      "--fallback-url",
      default="http://localhost:8080/elm",
      help="Rollback target URL.",
  )
  parser.add_argument("--probe-user-id", type=int, default=1, help="User id for probe API.")
  parser.add_argument("--timeout", type=int, default=5, help="HTTP timeout in seconds.")
  parser.add_argument("--skip-verify", action="store_true", help="Skip API probe before rollback.")
  return parser.parse_args()


def must_env(name: str) -> str:
  value = os.getenv(name)
  if value is None or value.strip() == "":
    raise SwitchError(f"Missing required env: {name}")
  return value.strip()


def main() -> int:
  args = parse_args()
  env_file = Path(args.env_file)
  if not env_file.exists():
    raise SwitchError(f"Env file not found: {env_file}")

  load_dotenv(env_file)
  token = must_env("INTERNAL_SERVICE_TOKEN")
  current_values = dotenv_values(env_file)
  old_url = (current_values.get("ACCOUNT_SERVICE_URL") or "").strip()
  fallback_url = args.fallback_url.strip()
  if not fallback_url:
    raise SwitchError("fallback-url is empty")

  verify_ok = None
  verify_msg = "skipped"
  if not args.skip_verify:
    verify_ok, verify_msg = probe_internal_account(
        fallback_url, token, args.probe_user_id, args.timeout
    )
    if not verify_ok:
      raise SwitchError(f"Probe fallback url failed: {verify_msg}")

  set_key(env_file, "ACCOUNT_SERVICE_URL_PREVIOUS", old_url)
  set_key(env_file, "ACCOUNT_SERVICE_URL", fallback_url)
  set_key(env_file, "ACCOUNT_GRAY_MODE", "rollback")

  print("\n=== ACCOUNT ROLLBACK ===")
  print(f"ENV_FILE={env_file}")
  print(f"OLD_URL={old_url}")
  print(f"ROLLBACK_URL={fallback_url}")
  print(f"VERIFY_OK={verify_ok}")
  print(f"VERIFY_MSG={verify_msg}")
  print("ROLLBACK_OK=True")
  return 0


if __name__ == "__main__":
  try:
    raise SystemExit(main())
  except SwitchError as err:
    print(f"ERROR={err}")
    raise SystemExit(2)
