# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

When communicating with user, always speak simplified Chinese.

## Project Overview

Full-stack food delivery platform, evolved across multiple architecture iterations:

| Version | Directory | Architecture |
|---------|-----------|-------------|
| v1.0 | `elm-v1.0/` | Spring Boot monolith (legacy) |
| v2.0 | `elm-v2.0/` | Monolith with internal service split |
| Microservice | `elm-microservice/` | 4-service split (points, account, catalog, order) + `elm-v2.0` as aggregation layer |
| Cloud-native | `elm-cloud/` | Full Spring Cloud microservices with 8 business services, Eureka, Config Server, Gateway |

The **active development target is `elm-cloud/`**. Frontend: Vue 3 + TypeScript (`elm-cloud/elm-frontend/`).

## Common Commands

### elm-cloud (Spring Cloud Microservices)

```bash
# Build all services from elm-cloud/
cd elm-cloud && mvn clean package

# Build a single service
cd elm-cloud && mvn clean package -pl order-service -am

# Run all services via Docker
cd elm-cloud && docker compose up -d --build

# Stop Docker services
cd elm-cloud && docker compose down

# Clean restart (reset DB)
cd elm-cloud && docker compose down -v && docker compose up -d --build
```

### elm-v2.0 + elm-microservice (Docker)

```bash
# From repo root
docker compose up -d --build
docker compose down
docker compose down -v   # also wipe DB volumes
```

### Backend (elm-v2.0/, standalone)

```bash
cd elm-v2.0
mvn clean package
mvn spring-boot:run
mvn test
docker build -f Dockerfile.local -t elm-backend:local .
```

### Frontend (elm-frontend/ or elm-cloud/elm-frontend/)

```bash
pnpm install
pnpm dev
pnpm build
pnpm lint
pnpm format
pnpm type-check
```

### Code Quality

```bash
pre-commit run -a
pre-commit install
```

## elm-cloud Architecture (Active Development)

### Infrastructure

- **Eureka Server** (`eureka-server/`, port 8761): Service registry, all services register here
- **Config Server** (`config-server/`, port 8888): Centralized configuration via Spring Cloud Config. Supports `native` profile (local files in `config/`) and `git` profile (remote Git repo). Two instances for HA
- **Gateway** (`gateway/`, port 8080): Spring Cloud Gateway, single entry point. Routes requests to services via Eureka load-balancing (`lb://service-name`)
- **RabbitMQ**: Message broker for Spring Cloud Bus (config refresh broadcasting)

### Business Services

Each service has its own MySQL schema, registers with Eureka, and pulls config from Config Server:

| Service | Package | Key Responsibilities |
|---------|---------|---------------------|
| `user-service` | `cn.edu.tju.user` | Auth (JWT), user/role management |
| `address-service` | `cn.edu.tju.address` | Delivery addresses |
| `order-service` | `cn.edu.tju.order` | Orders, order items, reviews |
| `cart-service` | `cn.edu.tju.cart` | Shopping cart |
| `merchant-service` | `cn.edu.tju.merchant` | Business/merchant profiles |
| `product-service` | `cn.edu.tju.product` | Food items, categories |
| `wallet-service` | `cn.edu.tju.wallet` | Wallet balance, transactions, vouchers |
| `points-service` | `cn.edu.tju.points` | Points accounts, FIFO batches |

### Gateway Routes

Routes are defined in `elm-cloud/config/gateway.yml` (centralized) and `gateway/src/main/resources/application.yml` (local fallback). Key routing:

- `/elm/api/authenticate`, `/elm/api/register`, `/elm/api/users/**` → `user-service`
- `/elm/api/wallet/**`, `/elm/api/vouchers/**` → `wallet-service`
- `/elm/api/orders/**` → `order-service`
- `/elm/api/addresses/**` → `address-service`
- `/elm/api/businesses/**` → `merchant-service`
- `/elm/api/foods/**` → `product-service`
- `/elm/api/carts/**` → `cart-service`
- `/elm/api/points/**` → `points-service`

### Config Refresh (Spring Cloud Bus)

Config changes pushed to Config Server can be broadcast to all services without restart:

```bash
# Refresh a specific service
curl -X POST http://localhost:<port>/actuator/bus-refresh

# Refresh all services via Config Server
curl -X POST http://localhost:8888/actuator/bus-refresh
```

Mechanism: Config Server → RabbitMQ → all subscribed services pick up new `@RefreshScope` properties.

### Resilience Patterns

- **Circuit Breaker**: Resilience4j on service-to-service calls (see `order-service` for examples)
- **Load Balancing**: Spring Cloud LoadBalancer via Eureka (`lb://service-name`)

### Technology Stack

- Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- MySQL 8.0 (one schema per service)
- Docker Compose for local deployment
- Maven multi-module project (`elm-cloud/pom.xml` as parent)

## elm-v2.0 + elm-microservice Architecture

`elm-v2.0/` serves as the external API aggregation layer, calling 4 internal microservices via RestTemplate:

- **order-service**: Orders, order items, addresses, cart, reviews
- **catalog-service**: Merchants, foods, inventory pre-hold/release
- **account-service**: Wallet, transactions, voucher redemption/rollback
- **points-service**: Points accounts, transactions, rules

Service ports: v2.0 on 8080, points on 8081, account on 8082, catalog on 8083, order on 8084.

## Backend Structure (Applies to elm-v2.0 and elm-cloud services)

Standard layered architecture within each service:

- **Controller Layer**: REST endpoints (`/api/*`)
- **Service Layer**: Business logic with `@Transactional`
- **Repository Layer**: JPA repositories
- **Security**: JWT-based authentication with Spring Security
- **Model**: `model.BO` (entities), `model.VO` (DTOs)

Key shared package in `elm-v2.0`: `cn.edu.tju.core` — auth, security, base models.

## Frontend Structure (elm-cloud/elm-frontend/)

- **Vue 3 + TypeScript**: Composition API with `<script setup>`
- **Pinia**: State management (auth, cart, wallet, business)
- **Vue Router**: Multi-role routing (customer/merchant/admin)
- **Element Plus**: UI component library
- **Axios**: HTTP client with interceptors

Key directories: `src/api/`, `src/store/`, `src/views/`, `src/components/`, `src/router/`

## Key Business Systems

**Points System**: FIFO batch management with expiration. Earned from orders (ratio-based) and reviews (fixed). Frozen during checkout, deducted on completion, rolled back on cancellation.

**Wallet System**: Balance with credit limit. Transaction types: TOP_UP, WITHDRAW, PAYMENT.

**Voucher System**: Public vouchers (templates with stock/user limits) and private vouchers (user-owned, with expiration). Applied at checkout with threshold validation.

**Order Lifecycle**: PAID(1) → ACCEPTED(2) → DELIVERY(3) → COMPLETE(4) → COMMENTED(5). CANCELED(0) not implemented yet.

## Important Patterns

### Entity Management

`EntityUtils` provides: `setNewEntity()`, `updateEntity()`, `deleteEntity()` (soft delete), `filterEntity()` (exclude deleted).

### Error Handling

Custom exceptions with error codes: `PointsException`, `WalletException`, `TransactionException`, `PrivateVoucherException`, `PublicVoucherException`.

### Security

- JWT tokens: `Authorization: Bearer <token>`
- Role-based access: `@PreAuthorize("hasAuthority('ADMIN')")`
- Current user: `userService.getUserWithAuthorities()`

### elm-v2.0 Internal Service Communication

Use `InternalServiceClient` for cross-service calls (points notifications for order/review success). Avoids circular dependencies.

## Development Notes

- TypeScript: Use `unknown` instead of `any`, proper type guards for errors
- Vue: Prefer Composition API with `<script setup lang="ts">`
- Backend transactions: Use `@Transactional` for multi-step operations
- Pre-commit hooks: Prettier, ESLint, trailing whitespace removal
- elm-cloud DB init: scripts in `elm-cloud/docker/mysql/init/`, one schema per service
- Config Server in `native` mode reads from `elm-cloud/config/` directory
