# Stage 0 Baseline (2026-03-20)

This file freezes the monolith API contract and routing baseline before service extraction.

## API Contract Freeze

- Snapshot file: `docs/contracts/stage0-monolith-openapi-2026-03-20.json`
- Source file: `elm-v2.0/interfaces.openapi.json`
- Compatibility rule: external API paths remain under `/elm/api/**`
- Controller-level OpenAPI groups are enabled via `SwaggerConfig`:
  - Endpoint pattern: `/elm/v3/api-docs/{group}`
  - Example groups: `controller-order`, `controller-points`, `controller-authentication`

## Gateway Skeleton

Gateway config is implemented in `elm-frontend/nginx.conf` with these logical upstreams:

- `iam_service`
- `points_service`
- `account_service`
- `catalog_service`
- `order_service`

All of them currently route to the monolith backend (`backend:8080`) for zero-downtime staging.

## Identity Context

JWT now includes claim `uid` (user id), in addition to existing claims:

- `sub`
- `auth`
- `exp`

Business services can read user identity from token claims directly when domain splitting starts.

## Logging Context

Unified log fields are now available through MDC:

- `traceId`
- `spanId`
- `requestId`
- `userId`
- `orderId`
