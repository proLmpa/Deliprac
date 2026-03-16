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
│               │  │  - Reviews    │  │  - Statistics   │
│               │  │  - Statistics │  │                 │
└───────────────┘  └───────────────┘  └─────────────────┘
        │
        │    JWT secret (shared key)
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

Each service owns its own PostgreSQL database. All requests originate from the client — no service-to-service calls. Foreign-key-like references across services (e.g. `store_id` in `orders`) are plain `BIGINT` columns — no ORM join, no FK constraint across DB boundaries.

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
│   ├── security/
│   │   ├── UserPrincipal.kt          # data class(id, email, role)
│   │   ├── UserRole.kt               # enum: CUSTOMER | OWNER | ADMIN
│   │   └── JwtAuthenticationFilter.kt
│   ├── exception/
│   │   └── GlobalExceptionHandler.kt # IllegalArgumentException→400, IllegalStateException→409
│   └── Extensions.kt                 # Optional.orThrow(msg)
│
├── user-service/         # Port 8081 — auth & user management
├── store-service/        # Port 8082 — stores, products, reviews
├── order-service/        # Port 8083 — cart, orders, statistics
│
├── docker-compose.yml    # Three PostgreSQL instances
└── http/                 # IntelliJ HTTP client request files
```

Package roots: `user.*`, `store.*`, `order.*`, `common.*`
Layout per service: `{root}.{layer}.{subdomain}` (e.g. `store.service.product`, `order.api.cart`)

---

## API Reference

All read operations use `POST` with a JSON request body. Mutation operations use `POST` (create), `PUT` (update/action), or `DELETE`.

### user-service · `:8081`

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/users/signup` | Public | `{ email, password, phone?, role? }` → `{ id }` |
| `POST` | `/api/users/signin` | Public | `{ email, password }` → `{ accessToken, tokenType }` |
| `PUT`  | `/api/users/{id}/suspend` | ADMIN | Suspend user (sets status `SUSPENDED`) |
| `PUT`  | `/api/users/me/withdraw` | Any | Self-withdraw (sets status `WITHDRAWN`) |

---

### store-service · `:8082`

#### Stores

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/stores` | OWNER | `{ name, address, phone, content, storePictureUrl?, productCreatedTime, openedTime, closedTime, closedDays }` |
| `POST` | `/api/stores/list` | Any | `{ sortBy: "CREATED_AT"\|"RATING" }` → `List<StoreResponse>` |
| `POST` | `/api/stores/mine` | OWNER | *(no body)* → `List<StoreResponse>` |
| `POST` | `/api/stores/find` | Any | `{ id }` → `StoreResponse` |
| `PUT`  | `/api/stores/{id}` | OWNER | `{ name, address, phone, ... }` |
| `PUT`  | `/api/stores/{id}/deactivate` | OWNER | Soft-delete (sets status `INACTIVE`) |

#### Products

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/stores/{storeId}/products` | OWNER | `{ name, description, price, productPictureUrl? }` |
| `POST` | `/api/stores/products/list` | Any | `{ storeId }` → `List<ProductResponse>` |
| `POST` | `/api/stores/products/find` | Any | `{ storeId, productId }` → `ProductResponse` |
| `PUT`  | `/api/stores/{storeId}/products/{productId}` | OWNER | `{ name, description, price, productPictureUrl? }` |
| `PUT`  | `/api/stores/{storeId}/products/{productId}/deactivate` | OWNER | Deactivate product |

#### Reviews

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST`   | `/api/stores/{storeId}/reviews` | CUSTOMER | `{ rating, content }` |
| `POST`   | `/api/stores/reviews/list` | Any | `{ storeId }` → `List<ReviewResponse>` |
| `DELETE` | `/api/stores/{storeId}/reviews/{reviewId}` | CUSTOMER (own) / ADMIN | — |

#### Statistics

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/stores/statistics/popular-products` | OWNER | `{ storeId }` → `List<ProductResponse>` ordered by popularity |
| `PUT`  | `/api/stores/{storeId}/products/{productId}/popularity` | OWNER | `?delta=N` — increment popularity |

---

### order-service · `:8083`

#### Cart

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST`   | `/api/carts` | CUSTOMER | `{ productId, storeId, unitPrice, quantity }` — creates cart if none; resets if different store |
| `POST`   | `/api/carts/me` | CUSTOMER | *(no body)* → current cart |
| `DELETE` | `/api/carts/{cartId}/products/{productId}` | CUSTOMER | Remove one item |
| `DELETE` | `/api/carts/{cartId}` | CUSTOMER | Clear all items |
| `PUT`    | `/api/carts/{cartId}/checkout` | CUSTOMER | Creates `Order(PENDING)` |

#### Orders

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/stores/orders/list` | OWNER | `{ storeId }` → `List<OrderResponse>` |
| `PUT`  | `/api/stores/{storeId}/orders/{orderId}/sold` | OWNER | PENDING → SOLD |
| `PUT`  | `/api/stores/{storeId}/orders/{orderId}/cancel` | OWNER | PENDING → CANCELED |
| `POST` | `/api/users/me/orders` | CUSTOMER | *(no body)* → `List<OrderResponse>` |

#### Statistics

| Method | Path | Auth | Body / Notes |
|--------|------|------|--------------|
| `POST` | `/api/stores/statistics/revenue` | OWNER | `{ storeId, year, month, timezone? }` → `RevenueResponse` |
| `POST` | `/api/users/me/statistics/spending` | CUSTOMER | `{ year, month, timezone? }` → `SpendingResponse` |

---

## API Flow Examples

### New customer places an order

```
1. POST /api/users/signup          → register account
2. POST /api/users/signin          → receive JWT

3. POST /api/stores/list           → browse stores  { sortBy: "RATING" }
4. POST /api/stores/products/list  → view products  { storeId: 5 }

5. POST /api/stores/products/find  → get price      { storeId: 5, productId: 12 }
   POST /api/carts                 → add item       { productId: 12, storeId: 5, unitPrice: 9000, quantity: 2 }

6. PUT  /api/carts/{cartId}/checkout → Order(PENDING) created

7. POST /api/users/me/orders       → confirm order in history
```

### Owner manages a store order

```
1. POST /api/users/signin          → receive JWT (role: OWNER)

2. POST /api/stores/orders/list    → see incoming orders  { storeId: 5 }

3. PUT  /api/stores/5/orders/{orderId}/sold
   → status: PENDING → SOLD

4. PUT  /api/stores/5/products/{productId}/popularity?delta=2
   → increment popularity for each sold item

5. POST /api/stores/statistics/revenue → check monthly revenue { storeId: 5, year: 2026, month: 3 }
```

---

## Key Design Decisions

### All reads use POST + JSON body
Path variables and query params for read operations are moved into the request body. This eliminates resource IDs from URLs on query-only endpoints (e.g. `POST /api/stores/find` with `{ "id": 5 }` instead of `GET /api/stores/5`).

### Multiple carts per user, one active at a time
`carts.user_id` is non-unique. A user accumulates carts over time; only the one with `is_ordered = false` is the active cart (queried via `findByUserIdAndIsOrderedFalse`). Ordered carts are preserved as history, permanently linked to their order. When a customer adds a product from a different store, the active cart is reset in-place — items cleared, `store_id` updated.

### Order status flow
```
PENDING ──► SOLD
        └─► CANCELED
```
Only `PENDING` orders can transition.

### Popularity tracking
`products.popularity` is a `BIGINT` incremented by the client calling `PUT /api/stores/{storeId}/products/{productId}/popularity?delta=N` after marking an order `SOLD`. Queried via QueryDSL (`ORDER BY popularity DESC`) to surface popular items.

### No shared database
Each service has its own PostgreSQL instance. Foreign-key-like references across services (e.g. `store_id` in `orders`) are plain `BIGINT` columns — no ORM join, no FK constraint across DB boundaries.

### Long for all monetary and accumulative fields
`price`, `unit_price`, `total_price`, `popularity`, `quantity`, `total_revenue`, `total_spending` are all `BIGINT` / `Long` to prevent integer overflow on aggregates.

### Statistics via QueryDSL with timezone support
Monthly aggregates compute UTC epoch-millis boundaries from a caller-supplied timezone:
```kotlin
ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
```

---

## Database Schema

```
user-service DB (port 5433)
┌────────────────────┐
│       users        │
│────────────────────│
│ id       BIGSERIAL │
│ email    VARCHAR   │
│ password VARCHAR   │
│ phone    VARCHAR   │
│ role     VARCHAR   │  ← CUSTOMER | OWNER | ADMIN
│ status   VARCHAR   │  ← ACTIVE | SUSPENDED | WITHDRAWN
│ created_at BIGINT  │
│ updated_at BIGINT  │
└────────────────────┘

store-service DB (port 5434)
┌────────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│       stores       │    │       products       │    │       reviews       │
│────────────────────│    │──────────────────────│    │─────────────────────│
│ id       BIGSERIAL │◄───│ store_id   BIGINT FK │    │ id        BIGSERIAL │
│ user_id* BIGINT    │    │ id         BIGSERIAL │    │ store_id* BIGINT FK │
│ name     VARCHAR   │    │ name       VARCHAR   │    │ user_id*  BIGINT    │
│ address  VARCHAR   │    │ description TEXT     │    │ rating    INT       │
│ status   VARCHAR   │    │ price      BIGINT    │    │ content   TEXT      │
│ ...      ...       │    │ popularity BIGINT    │    │ created_at BIGINT   │
└────────────────────┘    │ status     BOOLEAN  │    └─────────────────────┘
                          │ ...        ...      │
                          └──────────────────────┘

order-service DB (port 5435)
┌──────────────────┐    ┌─────────────────────────┐    ┌─────────────────────┐
│      carts       │    │      cart_products       │    │       orders        │
│──────────────────│    │─────────────────────────│    │─────────────────────│
│ id    BIGSERIAL  │◄───│ cart_id    BIGINT FK    │    │ id       BIGSERIAL  │
│ user_id* BIGINT  │    │ product_id* BIGINT      │    │ cart_id* BIGINT FK  │
│ store_id* BIGINT │    │ quantity   BIGINT        │    │ user_id* BIGINT     │
│ is_ordered BOOL  │    │ unit_price BIGINT        │    │ store_id* BIGINT    │
│ created_at BIGINT│    └─────────────────────────┘    │ total_price BIGINT  │
└──────────────────┘                                   │ status   VARCHAR    │
  user_id indexed,                                     └─────────────────────┘
  non-unique (1:N)
* cross-service reference — plain BIGINT, no FK constraint
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
./gradlew :store-service:test   # single module
```
