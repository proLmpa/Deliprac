# Baemin — Food Delivery Backend

A microservices backend inspired by Baemin (배달의민족), South Korea's largest food delivery platform.
Built with **Kotlin 2.x + Spring Boot 4** as a hands-on microservices architecture project.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
└────────────┬───────────────┬────────────────┬───────────┘
             │               │                │
     :8081   │       :8082   │        :8083   │
┌────────────▼──┐  ┌─────────▼─────┐  ┌──────▼──────────┐
│  user-service │  │ store-service │  │  order-service  │
│               │  │               │  │                 │
│  - Auth       │  │  - Stores     │  │  - Cart         │
│  - JWT issue  │  │  - Products   │  │  - Orders       │
│               │  │  - Statistics │  │  - Statistics   │
└───────────────┘  └───────────────┘  └────────┬────────┘
        │                  ▲                    │
        │    JWT secret     │  /internal/        │
        │   (shared key)    └────────────────────┘
        │
┌───────▼───────┐  ┌───────────────┐  ┌─────────────────┐
│  userdb :5433 │  │ storedb :5434 │  │  orderdb :5435  │
│  (PostgreSQL) │  │ (PostgreSQL)  │  │  (PostgreSQL)   │
└───────────────┘  └───────────────┘  └─────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    common (library)                     │
│  UserPrincipal · UserRole · JwtAuthenticationFilter     │
│  GlobalExceptionHandler · Extensions                    │
└─────────────────────────────────────────────────────────┘
```

Each service owns its own PostgreSQL database. Cross-service calls (order-service → store-service) use HTTP via `RestClient` against `/internal/**` endpoints — no shared DB, no ORM join across boundaries.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2 |
| Framework | Spring Boot 4.0 |
| Security | Spring Security 7, JWT (jjwt 0.12) |
| Persistence | Spring Data JPA, Hibernate, QueryDSL 5.1 |
| Database | PostgreSQL 16 |
| Build | Gradle 9 (Kotlin DSL), kapt |
| Java | JDK 24 |
| Testing | JUnit 5, Mockito 5, MockMvc |
| Infrastructure | Docker Compose |

---

## Module Structure

```
Baemin/
├── common/               # Shared library (not a Spring Boot app)
│   └── security/
│       ├── UserPrincipal.kt          # data class(id, email, role)
│       ├── UserRole.kt               # enum: CUSTOMER | OWNER | ADMIN
│       └── JwtAuthenticationFilter.kt
│   └── exception/
│       └── GlobalExceptionHandler.kt # IllegalArgumentException→400, IllegalStateException→409
│   └── Extensions.kt                 # Optional.orThrow(msg)
│
├── user-service/         # Port 8081 — auth & user management
├── store-service/        # Port 8082 — stores & products
├── order-service/        # Port 8083 — cart, orders, statistics
│
├── docker-compose.yml    # Three PostgreSQL instances
└── http/                 # IntelliJ HTTP client request files
```

Package convention inside each service: `com.example.baemin.{layer}.{subdomain}`
(e.g., `service.product`, `repository.cart`, `api.order`)

---

## Services & API Endpoints

### user-service · `:8081`

Authentication is public; all other services require a JWT.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/users/signup` | Public | Register. Body includes optional `role` (default `CUSTOMER`) |
| `POST` | `/api/users/signin` | Public | Login → returns `{ accessToken, tokenType }` |

**User roles:** `CUSTOMER` · `OWNER` · `ADMIN`

---

### store-service · `:8082`

#### Stores

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/stores` | OWNER | Create a store |
| `GET` | `/api/stores` | Any | List all active stores |
| `GET` | `/api/stores/mine` | OWNER | List caller's own stores |
| `GET` | `/api/stores/{id}` | Any | Get store detail |
| `PUT` | `/api/stores/{id}` | OWNER | Update store info (must own) |
| `PUT` | `/api/stores/{id}/deactivate` | OWNER | Soft-delete store (sets status `INACTIVE`) |

#### Products

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/stores/{storeId}/products` | OWNER | Register a product (starts active, popularity 0) |
| `GET` | `/api/stores/{storeId}/products` | Any | List store products |
| `GET` | `/api/stores/{storeId}/products/{productId}` | Any | Get product detail |
| `PUT` | `/api/stores/{storeId}/products/{productId}` | OWNER | Update product (must own store) |
| `PUT` | `/api/stores/{storeId}/products/{productId}/deactivate` | OWNER | Deactivate product |

#### Statistics & Internal

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/stores/{storeId}/statistics/popular-products` | OWNER | Top product by popularity |
| `GET` | `/internal/products/{productId}` | Internal | Product info for order-service |
| `PUT` | `/internal/products/{productId}/popularity` | Internal | Increment popularity on sale |

---

### order-service · `:8083`

#### Cart

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/carts` | CUSTOMER | Add item to cart. Creates cart if none; replaces if different store |
| `GET` | `/api/carts` | CUSTOMER | View current cart |
| `DELETE` | `/api/carts/{cartId}/products/{productId}` | CUSTOMER | Remove one item |
| `DELETE` | `/api/carts/{cartId}` | CUSTOMER | Clear all items |
| `PUT` | `/api/carts/{cartId}/checkout` | CUSTOMER | Checkout → creates an Order (`PENDING`) |

#### Orders

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/stores/{storeId}/orders` | OWNER | List store's orders |
| `PUT` | `/api/stores/{storeId}/orders/{orderId}/sold` | OWNER | Mark order `SOLD` (increments product popularity) |
| `PUT` | `/api/stores/{storeId}/orders/{orderId}/cancel` | OWNER | Mark order `CANCELED` |
| `GET` | `/api/users/me/orders` | CUSTOMER | List caller's orders |
| `GET` | `/internal/orders/{orderId}` | Internal | Order detail for service calls |

#### Statistics

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/stores/{storeId}/statistics/revenue?year=&month=` | OWNER | Monthly revenue (SOLD orders only) |
| `GET` | `/api/users/me/statistics/spending?year=&month=` | CUSTOMER | Monthly spending (SOLD orders only) |

---

## Key Design Decisions

### One cart per user, replace on conflict
`carts.user_id` is UNIQUE. When a customer adds a product from a different store (or re-adds after checkout), the cart is reset **in-place** — items cleared, `store_id` updated, `is_ordered` reset — rather than deleted, because `orders.cart_id` holds a FK reference.

### Order status flow
```
PENDING ──► SOLD
        └─► CANCELED
```
Only `PENDING` orders can transition. `markSold` also calls store-service to increment each product's popularity score.

### Popularity tracking
`products.popularity` is a plain integer incremented via an internal HTTP call from order-service whenever an order is marked `SOLD`. Queried via QueryDSL (`ORDER BY popularity DESC`) to surface popular items.

### Cross-service communication
order-service calls store-service synchronously using Spring's `RestClient`:
- `GET /internal/products/{id}` — validate product is active + get price snapshot
- `PUT /internal/products/{id}/popularity` — increment on sale

Internal endpoints are `permitAll()` in SecurityConfig (no JWT required between services).

### No shared database
Each service has its own PostgreSQL instance. Foreign-key-like references across services (e.g., `store_id` in `orders`) are plain `BIGINT` columns — no ORM join, no FK constraint across DB boundaries.

### Statistics via QueryDSL
Monthly aggregates use QueryDSL with UTC epoch-millis boundaries:
```kotlin
val from = LocalDate.of(year, month, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
val to   = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
```

---

## Getting Started

### Prerequisites
- Docker + Docker Compose
- JDK 24

### 1. Start databases

```bash
docker compose up -d
```

| Service | Container | Port | DB | User | Password |
|---|---|---|---|---|---|
| user-service | baemin_user_db | 5433 | userdb | user_svc | userpass |
| store-service | baemin_store_db | 5434 | storedb | store_svc | storepass |
| order-service | baemin_order_db | 5435 | orderdb | order_svc | orderpass |

### 2. Apply schemas

```bash
docker compose exec -T user-db  psql -U user_svc  -d userdb  < user-service/src/main/resources/db/schema.sql
docker compose exec -T store-db psql -U store_svc -d storedb < store-service/src/main/resources/db/schema.sql
docker compose exec -T order-db psql -U order_svc -d orderdb < order-service/src/main/resources/db/schema.sql
```

### 3. Run services

```bash
# Each in a separate terminal
./gradlew :user-service:bootRun
./gradlew :store-service:bootRun
./gradlew :order-service:bootRun
```

### 4. Run tests

```bash
./gradlew test                  # all modules
./gradlew :order-service:test   # single module
```

---

## Database Schema Overview

```
user-service DB                store-service DB
┌──────────┐                   ┌──────────┐    ┌──────────────┐
│  users   │                   │  stores  │    │   products   │
│──────────│                   │──────────│    │──────────────│
│ id       │                   │ id       │◄───│ store_id     │
│ email    │                   │ user_id* │    │ name         │
│ password │                   │ name     │    │ price        │
│ phone    │                   │ address  │    │ popularity   │
│ role     │                   │ status   │    │ status(bool) │
│ status   │                   │ ...      │    │ ...          │
└──────────┘                   └──────────┘    └──────────────┘

order-service DB
┌──────────┐    ┌───────────────┐    ┌──────────┐
│  carts   │    │ cart_products │    │  orders  │
│──────────│    │───────────────│    │──────────│
│ id       │◄───│ cart_id       │    │ id       │
│ user_id* │    │ product_id*   │    │ cart_id* │◄── carts.id
│ store_id*│    │ quantity      │    │ user_id* │
│is_ordered│    │ unit_price    │    │ store_id*│
└──────────┘    └───────────────┘    │ status   │
                                     └──────────┘
* cross-service reference — plain BIGINT, no FK constraint
```

---

## Implementation Status

| Domain | Service | Status |
|---|---|:---:|
| User (auth) | user-service | ✅ |
| Store | store-service | ✅ |
| Product | store-service | ✅ |
| Review | store-service | ❌ |
| Cart | order-service | ✅ |
| Order | order-service | ✅ |
| Statistics | order-service / store-service | ✅ |
