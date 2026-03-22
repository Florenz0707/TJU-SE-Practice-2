# Four-Service Smoke Scripts (uv)

## 1. Prepare env and dependencies

```bash
cd elm-v2.0/scripts
cp integration.env.example .env
# edit .env locally (DB creds/internal token)
uv sync
```

## 2. Run smoke

```bash
cd elm-v2.0/scripts
uv run run_four_service_smoke.py
```

## 3. Run phase3 account drill

```bash
cd elm-v2.0/scripts
uv run run_phase3_account_drill.py --env-file .env
```

## 4. Account gray switch and rollback

```bash
cd elm-v2.0/scripts

# show current ACCOUNT_SERVICE_URL and probe status
uv run manage_account_gray.py status --env-file .env

# switch to account-service (canary/full are equivalent at env level)
uv run manage_account_gray.py switch --env-file .env --mode canary --target-url http://localhost:8082/elm

# rollback to previous URL (ACCOUNT_SERVICE_URL_PREVIOUS)
uv run rollback_account_gray.py --env-file .env
```

## 5. Useful options

```bash
# use a custom env file
uv run run_four_service_smoke.py --env-file /path/to/custom.env

# skip service startup and only run smoke checks against already running services
uv run run_four_service_smoke.py --skip-start

# skip account-service startup and drill against already running account-service
uv run run_phase3_account_drill.py --skip-start

# skip probe before switch (not recommended)
uv run manage_account_gray.py switch --mode rollback --skip-verify
```

## 6. Output and logs

- script log output: terminal
- service logs: `elm-v2.0/scripts/logs/*.log`

## 7. Security notes

- keep secrets only in `elm-v2.0/scripts/.env` (already git-ignored)
- do not commit real DB credentials or internal token
