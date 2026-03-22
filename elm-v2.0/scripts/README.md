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

## 3. Useful options

```bash
# use a custom env file
uv run run_four_service_smoke.py --env-file /path/to/custom.env

# skip service startup and only run smoke checks against already running services
uv run run_four_service_smoke.py --skip-start
```

## 4. Output and logs

- script log output: terminal
- service logs: `elm-v2.0/scripts/logs/*.log`

## 5. Security notes

- keep secrets only in `elm-v2.0/scripts/.env` (already git-ignored)
- do not commit real DB credentials or internal token
