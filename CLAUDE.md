# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

When communicating with user, always speak simplified Chinese.

## Project Overview

Full-stack food delivery platform with Spring Boot backend and Vue 3 frontend. Supports customer orders, merchant management, admin operations, with integrated points, wallet, and voucher systems.

## Common Commands

### Backend (elm-v2.0/)

```bash
# Build
mvn clean package

# Run locally
mvn spring-boot:run

# Run tests
mvn test

# Docker build
docker build -f Dockerfile.local -t elm-backend:local .
```

### Frontend (elm-frontend/)

```bash
# Install dependencies
pnpm install

# Development server
pnpm dev

# Build for production
pnpm build

# Lint and format
pnpm lint
pnpm format

# Type check
pnpm type-check
```

### Code Quality

```bash
# Run all pre-commit hooks
pre-commit run -a

# Install hooks
pre-commit install
```

## Architecture

### Backend Structure

- **Controller Layer**: REST endpoints (`/api/*`)
- **Service Layer**: Business logic with transaction management
- **Repository Layer**: JPA repositories for data access
- **Security**: JWT-based authentication with Spring Security
- **Database**: H2 in-memory (dev), configurable for production

Key packages:

- `cn.edu.tju.core`: Authentication, security, base models
- `cn.edu.tju.elm.controller`: API endpoints
- `cn.edu.tju.elm.service`: Business logic
- `cn.edu.tju.elm.repository`: Data access
- `cn.edu.tju.elm.model.BO`: Business objects (entities)
- `cn.edu.tju.elm.model.VO`: View objects (DTOs)

### Frontend Structure

- **Vue 3 + TypeScript**: Composition API with `<script setup>`
- **Pinia**: State management (auth, cart, wallet, business)
- **Vue Router**: Multi-role routing (customer/merchant/admin)
- **Element Plus**: UI component library
- **Axios**: HTTP client with interceptors

Key directories:

- `src/api/`: API client functions
- `src/store/`: Pinia stores
- `src/views/`: Page components (customer/merchant/admin/mobile)
- `src/components/`: Reusable components
- `src/router/`: Route definitions and guards

### Multi-Role System

Three user roles with separate interfaces:

- **Customer**: Browse restaurants, place orders, manage wallet/points
- **Merchant**: Manage business profile, menu, orders
- **Admin**: User management, merchant approval, system configuration

### Key Business Systems

**Points System**:

- FIFO batch management with expiration
- Earned from orders (ratio-based) and reviews (fixed amount)
- Can be frozen during checkout, deducted on completion, or rolled back on cancellation
- Internal service integration via `InternalServiceClient`

**Wallet System**:

- Balance management with credit limit support
- Transaction types: TOP_UP, WITHDRAW, PAYMENT
- Integrated with order payments (tracked separately from transactions)

**Voucher System**:

- Public vouchers (templates) with stock and per-user limits
- Private vouchers (user-owned instances) with expiration
- Applied during checkout with threshold validation

**Order Lifecycle**:

1. PAID (1) - Created with payment
2. ACCEPTED (2) - Merchant accepts
3. DELIVERY (3) - Out for delivery
4. COMPLETE (4) - Delivered, points awarded
5. COMMENTED (5) - Customer reviewed
6. CANCELED (0) - Not implemented yet

## Important Patterns

### Entity Management

Use `EntityUtils` for consistent entity lifecycle:

- `setNewEntity()`: Set creation metadata
- `updateEntity()`: Set update metadata
- `deleteEntity()`: Soft delete
- `filterEntity()`: Remove deleted entities

### Error Handling

Custom exceptions with predefined error codes:

- `PointsException`, `WalletException`, `TransactionException`
- `PrivateVoucherException`, `PublicVoucherException`

### Security

- JWT tokens in `Authorization: Bearer <token>` header
- Role-based access: `@PreAuthorize("hasAuthority('ADMIN')")`
- Current user: `userService.getUserWithAuthorities()`

### Internal Service Communication

Use `InternalServiceClient` for cross-service calls:

- Points notifications (order success, review success)
- Avoids circular dependencies

## Development Notes

- TypeScript: Use `unknown` instead of `any`, proper type guards for errors
- Vue components: Prefer Composition API with `<script setup lang="ts">`
- Backend transactions: Use `@Transactional` for multi-step operations
- Pre-commit hooks enforce: Prettier, ESLint, trailing whitespace removal
- Docker: Multi-stage builds for both frontend and backend
