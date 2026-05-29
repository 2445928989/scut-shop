# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

SCUT-Shop is a full-stack online shopping website — Vue 3 + TypeScript frontend, Spring Boot 3.2 + MyBatis backend, orchestrated with Docker Compose. It's a university course project (network application architecture).

## Commands

```bash
# Full stack (Docker)
docker compose up -d --build          # start all services
docker compose down                   # stop all services
docker compose down -v               # stop and wipe DB volume

# Backend (cd backend)
mvn spring-boot:run                   # dev server (needs MySQL/Redis)
mvn spring-boot:run -Dspring-boot.run.profiles=e2e   # H2 in-memory, no external deps
mvn test                              # all tests
mvn -Dtest=IntegrationTest test       # single integration test

# Frontend (cd frontend)
npm install                           # first time only
npm run dev                           # Vite dev server on port 3000
npm run build                         # production build
npm run lint                          # ESLint
npm run test:unit                     # Vitest (single run)
npm run test:watch                    # Vitest (watch mode)
```

## Architecture

### Backend (Spring Boot 3.2 + MyBatis)

**Layers**: Controller → Service → Mapper → MySQL. Mappers use MyBatis annotations (inline SQL), not XML mapper files. The `@Mapper`-annotated interfaces live in `backend/src/main/java/com/scutshop/backend/mapper/`.

**Auth**: Stateless JWT. `JwtAuthenticationFilter` extracts Bearer tokens, `JwtTokenProvider` handles signing/validation. Tokens include roles as authorities. Passwords use a custom `FlexiblePasswordEncoder` that upgrades SHA-256 hashes to BCrypt on login. Refresh tokens are stored in the `refresh_token` table. Email activation is optional (toggled via `EMAIL_ACTIVATION_ENABLED` env var, uses MailHog in dev).

**Security**: `SecurityConfig` permits `/api/auth/**`, `/api/products/**`, `/api/cart/**`, `/api/uploads/**` without auth; admin endpoints use `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`. CORS allows all origins in dev.

**Database**: MySQL 8 with 12 tables — `user`, `role`, `user_role`, `product`, `product_audit`, `cart`, `cart_item`, `refresh_token`, `order`, `order_item`, `payment`, `user_log`. Init scripts at `db/init.sql` (schema + seed data) and `db/init_admin.sh` (admin user from `.env`). Product deletes are soft deletes (`status = 0`), recorded in `product_audit`.

**Guest carts**: Unauthenticated users get a `cartId` UUID stored in localStorage, sent via `X-Cart-Id` header. On login, guest carts merge into the user's cart.

**File uploads**: `FileUploadController` serves uploads from `uploads/` directory (mounted as a Docker volume). `WebConfig` maps `/api/uploads/**` to that directory.

**Profiles**: Default uses MySQL (from env vars). `e2e` profile uses H2 in-memory with MySQL compatibility mode, initialized by `src/main/resources/db/h2-init.sql`.

**Key env vars** (in `.env`, copied from `.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `MAIL_HOST`, `MAIL_SMTP_PORT`, `EMAIL_ACTIVATION_ENABLED`, `FRONTEND_BASE`, `PAYMENT_MOCK`.

### Frontend (Vue 3 + Element Plus + Pinia)

**Tech**: Vue 3, TypeScript, Vite 5, Pinia (state), Vue Router 4, Element Plus (UI), Axios (HTTP).

**Pages** (`frontend/src/pages/`): `Products.vue` (product listing), `ProductDetail.vue`, `Login.vue`, `Register.vue`, `Activate.vue`, `Cart.vue`, `Orders.vue`, `AdminProducts.vue`, `AdminOrders.vue`, `AdminReports.vue`, `AdminUsers.vue`.

**Stores** (`frontend/src/stores/`): `auth.ts` (JWT tokens, user info, login/register/refresh/logout), `cart.ts` (cart items, total price calc skips off-shelf items with `status === 0`).

**API layer** (`frontend/src/api/index.ts`): Single Axios instance. Request interceptor attaches Bearer token and guest `X-Cart-Id`. Response interceptor handles 401 by attempting token refresh via `/api/auth/refresh`, then retries the original request. Concurrent 401s are deduplicated through a shared refresh promise.

**Router** (`frontend/src/router/index.ts`): Auth guards prevent authenticated users from visiting login/register. Admin routes (`/admin/*`) check for `ROLE_ADMIN` via `fetchMe()` but don't block navigation — page components handle showing login prompts themselves.

**Nginx/production**: The Docker `frontend` service builds the Vite app and serves it via Nginx on port 3000 (container port 80). API requests are proxied through Nginx to the backend — the frontend Axios instance uses relative URLs (`VITE_API_BASE` empty by default).

### Infrastructure (Docker Compose)

Services: `db` (MySQL 8), `redis` (Redis 7), `mailhog` (dev mail catcher), `adminer` (DB browser on port 8080), `app` (Spring Boot), `frontend` (Nginx on port 3000). The `frontend` service depends on `app` being healthy (`/api/health` endpoint).

### Order/Payment flow

1. Cart items → `POST /api/orders/checkout` creates an `Order` with status `CREATED`, `Payment` with status `INIT`
2. `POST /api/orders/{id}/pay` — currently mock payment only (`PAYMENT_MOCK=true`), sets order to `PAID`, payment to `SUCCESS`
3. Admin advances order through `SHIPPED` → `DELIVERED` via `PUT /api/admin/orders/{id}`

## Testing

Backend tests use JUnit 5 (Testcontainers for MySQL integration tests). Frontend tests use Vitest + jsdom + Vue Test Utils + @testing-library/jest-dom. Test files follow `*.spec.ts` or `*.test.ts` naming under `src/`.
