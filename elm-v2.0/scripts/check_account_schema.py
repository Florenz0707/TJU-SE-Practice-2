#!/usr/bin/env python3
"""Check account-service schema readiness."""

from __future__ import annotations

import argparse
import os
from typing import Any

import pymysql
from dotenv import load_dotenv


REQUIRED_TABLES = {
    "wallet",
    "transaction",
    "public_voucher",
    "private_voucher",
}


class SchemaCheckError(RuntimeError):
  pass


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Check account-service schema readiness.")
  parser.add_argument("--env-file", default=".env", help="Path to env file.")
  return parser.parse_args()


def must_env(name: str) -> str:
  value = os.getenv(name)
  if value is None or value.strip() == "":
    raise SchemaCheckError(f"Missing required env: {name}")
  return value.strip()


def fetch_tables(database: str, mysql_cfg: dict[str, Any]) -> set[str]:
  conn = pymysql.connect(
      host=mysql_cfg["host"],
      port=mysql_cfg["port"],
      user=mysql_cfg["user"],
      password=mysql_cfg["password"],
      database=database,
      charset="utf8mb4",
      cursorclass=pymysql.cursors.DictCursor,
  )
  try:
    with conn.cursor() as cur:
      cur.execute("SHOW TABLES")
      rows = cur.fetchall()
      if not rows:
        return set()
      key = next(iter(rows[0].keys()))
      return {str(row[key]) for row in rows}
  finally:
    conn.close()


def main() -> int:
  args = parse_args()
  load_dotenv(args.env_file)

  mysql_cfg = {
      "host": must_env("MYSQL_HOST"),
      "port": int(must_env("MYSQL_PORT")),
      "user": must_env("MYSQL_USER"),
      "password": must_env("MYSQL_PASSWORD"),
  }
  db_account = must_env("DB_ACCOUNT")
  db_main = must_env("DB_MAIN")

  account_tables = fetch_tables(db_account, mysql_cfg)
  main_tables = fetch_tables(db_main, mysql_cfg)

  missing = sorted(REQUIRED_TABLES - account_tables)
  overlapping = sorted(REQUIRED_TABLES & main_tables)

  ok = len(missing) == 0
  print("\n=== ACCOUNT SCHEMA CHECK ===")
  print(f"DB_ACCOUNT={db_account}")
  print(f"DB_MAIN={db_main}")
  print(f"REQUIRED_TABLES={sorted(REQUIRED_TABLES)}")
  print(f"MISSING_IN_DB_ACCOUNT={missing}")
  print(f"OVERLAP_IN_DB_MAIN={overlapping}")
  print(f"ACCOUNT_SCHEMA_OK={ok}")
  return 0 if ok else 2


if __name__ == "__main__":
  try:
    raise SystemExit(main())
  except SchemaCheckError as err:
    print(f"ERROR={err}")
    raise SystemExit(2)
