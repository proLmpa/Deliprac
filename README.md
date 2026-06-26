# Baemin — Food Delivery Backend

A microservices backend inspired by Baemin (배달의민족), South Korea's largest food delivery platform.
Built with **Kotlin 2.x + Spring Boot 4** as a hands-on microservices architecture project.

---

## Architecture

```mermaid
flowchart TB
    C["Client\n(front-service)"]

    subgraph bff ["BFF Layer :8080"]
        BFF["BFF Server"]
    end

    subgraph svc ["Backend Services"]
        direction LR
        US["user-service :8081\nAuth · JWT issue"]
        SS["store-service :8082\nStores · Products · Reviews · Statistics"]
        OS["order-service :8083\nCart · Orders · Statistics"]
        NS["notification-service :8084\nNotifications"]
    end

    subgraph db ["PostgreSQL Databases"]
        direction LR
        UD[("userdb\n:5433")]
        SD[("storedb\n:5434")]
        OD[("orderdb\n:5435")]
        ND[("notificationdb\n:5436")]
    end

    RD[("Redis\n:6379")]

    subgraph mon ["Monitoring"]
        direction LR
        PR["Prometheus :9090"] --> AM["Alertmanager :9093"] --> TG(["Telegram"])
        PR --> GF["Grafana :3000"]
    end

    C --> BFF
    BFF --> US & SS & OS & NS

    US --> UD
    SS --> SD
    OS --> OD
    NS --> ND

    SS & OS & NS -->|cache| RD

    US & SS & OS & NS -->|metrics| PR
```

All client requests flow through the BFF, which aggregates cross-service calls and forwards them to the appropriate backend service. There are no direct service-to-service calls between backend services. Each service owns its own PostgreSQL database. Foreign-key-like references across services (e.g. `store_id` in `orders`) are plain `BIGINT` columns — no ORM join, no FK constraint across DB boundaries.

The JWT secret is shared across all services via configuration — user-service issues tokens, and store-service and order-service validate them independently using the same secret. No runtime call is made between services for authentication.

Every BFF→backend call is signed with HMAC-SHA256. The BFF attaches `X-Bff-Timestamp` and `X-Bff-Signature` headers to each outgoing `RestClient` request; each backend service validates the signature before Spring Security runs, rejecting direct callers with `401 Unauthorized`. Each service has its own independent HMAC secret.

Notifications are created asynchronously (fire-and-forget) by the BFF after order mutations. After a checkout the BFF looks up the store owner and calls notification-service to notify them; after marking an order sold or canceled it calls notification-service to notify the customer. The BFF calls `/internal/notifications` (no JWT) — notification-service treats the BFF as a trusted internal caller. The notification call runs in a background thread (`CompletableFuture.runAsync`) so failures are silent and do not affect the API response. Clients poll the REST endpoint to fetch and mark notifications.

### BFF Layer

The BFF (Backend for Frontend) sits between the client and the four backend services. Its responsibilities:

- **Routing** — forwards requests to the correct backend service
- **Aggregation** — combines data from multiple services into a single response (e.g. order list enriched with store/product names)
- **Auth delegation** — attaches the JWT from the client and forwards it; each backend service validates independently
- **Cross-service data hand-off** — handles flows where data from one service is needed as input to another (e.g. fetching `unitPrice` from store-service before calling order-service to add a cart item)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.21 |
| Framework | Spring Boot 4.0 |
| Security | Spring Security 7, JWT (jjwt 0.12) |
| Persistence | Spring Data JPA, Hibernate, QueryDSL 5.1 |
| Database | PostgreSQL 16 |
| Caching | Spring Cache + Redis (`@Cacheable` — store-service, order-service) · Caffeine L1 + Redis L2 (notification-service public notifications) |
| Resilience | Resilience4j 2.3 (circuit breaker) |
| Observability | Spring Boot Actuator, Micrometer, Prometheus, Grafana, Alertmanager |
| Build | Gradle 9 (Kotlin DSL), kapt |
| Java | JDK 25 |
| Testing | JUnit 5, Mockito 5, MockMvc |
| Infrastructure | Docker, Kubernetes (minikube), Helm, ArgoCD, Jenkins |

---

## Getting Started

### Prerequisites
- JDK 25

### 1. Apply schemas

```bash
psql -h 192.168.160.101 -U user_svc  -d userdb         < user-service/src/main/resources/db/schema.sql
psql -h 192.168.160.102 -U store_svc -d storedb        < store-service/src/main/resources/db/schema.sql
psql -h 192.168.160.103 -U order_svc -d orderdb        < order-service/src/main/resources/db/schema.sql
psql -h 192.168.160.104 -U notif_svc -d notificationdb < notification-service/src/main/resources/db/schema.sql
```

### 2. Run services

```bash
# Each in a separate terminal
./gradlew :user-service:bootRun
./gradlew :store-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :bff-service:bootRun
```

### 3. Run tests

```bash
./gradlew test                              # all modules
./gradlew :store-service:test               # single module
./gradlew :notification-service:test
```

---

## Key Design Decisions

### 1. All reads use POST + JSON body
Path variables and query params for read operations are moved into the request body. This eliminates resource IDs from URLs on query-only endpoints (e.g. `POST /api/stores/find` with `{ "id": 5 }` instead of `GET /api/stores/5`).

### 2. Multiple carts per user, one active at a time
`carts.user_id` is non-unique. A user accumulates carts over time; only the one with `is_ordered = false` is the active cart (queried via `findByUserIdAndIsOrderedFalse`). Ordered carts are preserved as history, permanently linked to their order. When a customer adds a product from a different store, the active cart is reset in-place — items cleared, `store_id` updated.

### 3. Order status flow

```mermaid
stateDiagram-v2
    [*] --> PENDING : PUT /carts/checkout
    PENDING --> SOLD      : PUT /stores/orders/sold
    PENDING --> CANCELED  : PUT /stores/orders/cancel
```

Only `PENDING` orders can transition.

### 4. Popularity tracking
`products.popularity` is a `BIGINT` incremented by the client calling `PUT /api/stores/products/popularity` with `{ storeId, productId, delta }` after marking an order `SOLD`. Queried via QueryDSL (`ORDER BY popularity DESC`) to surface popular items.

### 5. No shared database
Each service has its own PostgreSQL instance. Foreign-key-like references across services (e.g. `store_id` in `orders`) are plain `BIGINT` columns — no ORM join, no FK constraint across DB boundaries.

### 6. Long for all monetary and accumulative fields
`price`, `unit_price`, `total_price`, `popularity`, `quantity`, `total_revenue`, `total_spending` are all `BIGINT` / `Long` to prevent integer overflow on aggregates.

### 7. Statistics via QueryDSL with timezone support
Monthly aggregates compute UTC epoch-millis boundaries from a caller-supplied timezone:
```kotlin
ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
```

### 8. BFF-triggered fire-and-forget notifications
Notifications are created by the BFF immediately after order mutations — no message broker is involved. Every notification includes a full item snapshot (product name, unit price, quantity) and the store name.

| BFF action | Recipient | Type | Item source |
|---|---|---|---|
| `checkout` | Store owner | `NEW_ORDER` | BFF fetches active cart + product names from store-service |
| `markSold` | Customer | `ORDER_SOLD` | order-service returns cart snapshot in `OrderResponse.items` |
| `markCanceled` | Customer | `ORDER_CANCELED` | order-service returns cart snapshot in `OrderResponse.items` |

**userId security:** `StoreResponse.userId` and `OrderResponse.userId` are annotated `@get:JsonIgnore` / `@param:JsonProperty` — deserialized from backend services, never serialized to the frontend. The recipient's `userId` only ever exists inside the BFF at request time.

**Delivery:** notifications are created in a background thread (`CompletableFuture.runAsync`) after the BFF returns its response to the client. Failures are swallowed silently and do not affect the API caller. The frontend polls `/api/notifications/list` every 30 seconds; recipients see new notifications within one poll cycle assuming the async call succeeds.

### 9. Spring Cache (@Cacheable) in store-service and order-service

store-service and order-service cache their most-read query results in Redis using Spring's `@Cacheable` / `@CacheEvict` abstraction backed by `RedisCacheManager`.

| Cache name | Service | Cached method | Key | TTL |
|---|---|---|---|---|
| `stores` | store-service | `StoreService.findById` | `#id` | 5 min |
| `stores-all` | store-service | `StoreService.listAll` | `#sortBy` | 2 min |
| `products` | store-service | `ProductService.findById` | `#storeId + ':' + #productId` | 5 min |
| `products-by-store` | store-service | `ProductService.listByStore` | `#storeId` | 5 min |
| `orders-by-user` | order-service | `OrderService.listByUser` | `#userId` | 5 min |

Write methods (`create`, `update`, `deactivate`, `markSold`, `markCanceled`, `checkout`) carry matching `@CacheEvict` annotations so cached entries are invalidated immediately after a mutation.

**Serialization:** `GenericJacksonJsonRedisSerializer` writes a `@class` field into every JSON value so Jackson can reconstruct the correct concrete type on read. The serializer uses a dedicated `ObjectMapper` configured with `activateDefaultTypingAsProperty` (PROPERTY format). Passing Spring Boot's default `ObjectMapper` directly causes a `ClassCastException` because Jackson returns a `LinkedHashMap` without type information.

**Profile activation:** Redis connection properties are gated behind `cache-dev` / `cache-prod` profiles, which are included in the `dev` and `prod` profile groups respectively. The `test` group does not include a cache profile — `@WebMvcTest` slices never load `CacheConfig` and caching is transparent during testing.

---

### 10. Two-level cache for public notifications (Caffeine + Redis)

`POST /api/public-notifications/list` is called on every page load — including by unauthenticated visitors — making it the highest-frequency read endpoint in the system. To avoid a database hit on every request, notification-service uses a two-level read-through cache with write-through eviction.

**Read path (`listActive`)**

```
Request
  │
  ├─ L1 hit  → Caffeine (in-process, TTL 1 min)  ──────────────────────► return
  │
  ├─ L1 miss → Redis (shared, TTL 10 min) ──► repopulate Caffeine ──────► return
  │
  └─ L2 miss → PostgreSQL ──► write to Redis + Caffeine ─────────────────► return
```

**Write path (create / deactivate)**

Both mutations call `evictCache()` immediately after the DB write, invalidating both layers atomically:

```kotlin
private fun evictCache() {
    stringRedisTemplate.delete(cacheKey)   // evict Redis
    caffeineCache.invalidate(cacheKey)     // evict Caffeine
}
```

The next read after any mutation falls all the way through to the DB and repopulates both layers, ensuring stale data is never served after a change.

**Cache parameters**

| Layer | Implementation | TTL | Scope |
|---|---|---|---|
| L1 | Caffeine (`Cache<String, Any>`) | 1 minute | Per JVM instance |
| L2 | Redis (`StringRedisTemplate`, JSON) | 10 minutes | Shared across all instances |

The Caffeine TTL is intentionally shorter than Redis so that in a multi-instance deployment, stale L1 entries expire quickly while Redis continues to absorb DB traffic.

**Cache key:** `public-notifications:active` — the entire active list is stored as a single JSON array under one key. Invalidation is therefore O(1) regardless of how many notifications exist.

---

### 11. Resilience4j circuit breaker on NotificationClient

All four `NotificationClient` methods are annotated `@CircuitBreaker(name = "notification")`. Configuration (COUNT_BASED): sliding window 10, minimum calls 5, 50% failure threshold, 30 s open wait, 3 permitted calls in half-open, auto-transition enabled.

**Fallback behaviour:** the fire-and-forget `createNotification` logs a warning and returns silently; user-facing methods (`listMyNotifications`, `markRead`, `markAllRead`) return empty results or `Unit` so the API caller is unaffected. The circuit breaker state is exposed via `/actuator/prometheus` and triggers the Alertmanager `CircuitBreakerOpen` alert.

### 12. Monitoring — Prometheus, Grafana, Alertmanager

All five services expose Prometheus metrics at `/actuator/prometheus` (Spring Boot Actuator + Micrometer). A dedicated `vm-monitoring` VM runs all three tools natively as systemd services:

| Service | Package | Port | Role |
|---|---|---|---|
| `prometheus` | `prometheus` (apt) | 9090 | Scrapes `/actuator/prometheus` via minikube NodePorts every 15 s |
| `prometheus-alertmanager` | `prometheus-alertmanager` (apt) | 9093 | Routes firing alerts to Telegram |
| `grafana-server` | `grafana` (Grafana apt repo) | 3000 | Dashboard provisioned from `monitoring/grafana/` |

Alert rule: `CircuitBreakerOpen` — fires immediately when `resilience4j_circuitbreaker_state{state="open"} == 1`, sending a Telegram message via Alertmanager. Config files (`prometheus.yml`, `alertmanager.yml`) are templates with `${VAR}` placeholders substituted by `envsubst` on the Jenkins agent, then SCP'd to `/opt/monitoring/` on vm-monitoring. Jenkins reloads the services via `sudo systemctl reload`; no process restart is required for config-only changes.

### 13. BFF-to-backend HMAC signing

All four `RestClient` beans in the BFF have an `HmacSigningInterceptor` that signs every outgoing request. The signature covers method, path, timestamp, and a SHA-256 hash of the request body:

```
signature = HMAC-SHA256(secret, METHOD + "\n" + PATH + "\n" + TIMESTAMP_MS + "\n" + SHA256(body))
```

The result is sent as two headers: `X-Bff-Timestamp` (epoch milliseconds) and `X-Bff-Signature` (hex). Each backend service runs `HmacRequestFilter` at `HIGHEST_PRECEDENCE` — before Spring Security — and rejects any request that fails the following checks. `/actuator/**` paths are exempt from HMAC validation to allow Prometheus scraping without authentication.

| Check | Response |
|---|---|
| `X-Bff-Timestamp` or `X-Bff-Signature` missing | `401 Missing HMAC headers` |
| Timestamp outside ±30 s of server time | `401 Request expired` |
| Signature mismatch | `401 Invalid HMAC signature` |

Each service has its **own independent secret** (`bff.hmac.secret` in each backend, `bff.hmac.{service}.secret` in the BFF). A leaked secret for one service does not affect the others. In production, secrets are injected via environment variables (`BFF_HMAC_USER_SECRET`, `BFF_HMAC_STORE_SECRET`, etc.).

```mermaid
sequenceDiagram
    participant C as Client
    participant B as BFF :8080
    participant S as Backend :808x

    C->>B: POST /api/... (JWT)
    Note over B: HmacSigningInterceptor<br/>adds X-Bff-Timestamp + X-Bff-Signature
    B->>S: POST /api/... + HMAC headers
    Note over S: HmacRequestFilter<br/>validates before Spring Security
    S-->>B: 200 OK
    B-->>C: 200 OK
```

---

## API Flow Examples

### New customer places an order

```mermaid
sequenceDiagram
    actor Cu as Customer
    participant BFF as bff-service :8080
    participant SS as store-service :8082
    participant OS as order-service :8083
    participant NS as notification-service :8084

    Cu->>BFF: POST /api/users/signup
    BFF-->>Cu: 201 Created
    Cu->>BFF: POST /api/users/signin
    BFF-->>Cu: { accessToken }

    Cu->>BFF: POST /api/stores/list { sortBy: "RATING" }
    BFF-->>Cu: List[StoreResponse]
    Cu->>BFF: POST /api/stores/products/list { storeId }
    BFF-->>Cu: List[ProductResponse]

    Note over BFF,SS: BFF fetches unitPrice from store-service internally
    Cu->>BFF: POST /api/carts { productId, storeId, quantity }
    BFF->>SS: POST /api/stores/products/find { storeId, productId }
    SS-->>BFF: ProductResponse (price)
    BFF->>OS: POST /api/carts { productId, storeId, unitPrice, quantity }
    OS-->>BFF: CartResponse
    BFF-->>Cu: CartResponse

    Note over BFF,NS: BFF aggregates store + product info to build notification
    Cu->>BFF: PUT /api/carts/checkout { cartId }
    BFF->>OS: PUT /api/carts/checkout
    OS-->>BFF: OrderResponse (PENDING)
    BFF->>SS: POST /api/stores/find { storeId }
    SS-->>BFF: StoreResponse (owner userId, name)
    BFF->>SS: POST /api/stores/products/list { storeId }
    SS-->>BFF: List[ProductResponse] (names)
    BFF-->>Cu: OrderResponse (PENDING)
    Note over BFF,NS: async — fire and forget
    BFF-)NS: POST /internal/notifications (NEW_ORDER → owner, items)

    Cu->>BFF: POST /api/users/me/orders
    BFF-->>Cu: List[OrderResponse]
```

### Owner manages a store order

```mermaid
sequenceDiagram
    actor Ow as Owner
    actor Cu as Customer
    participant BFF as bff-service :8080
    participant SS as store-service :8082
    participant OS as order-service :8083
    participant NS as notification-service :8084

    Ow->>BFF: POST /api/users/signin
    BFF-->>Ow: { accessToken }

    Ow->>BFF: POST /api/stores/orders/list { storeId }
    BFF-->>Ow: List[OrderResponse]

    Note over BFF,NS: OrderResponse includes cart item snapshot (productId, unitPrice, quantity)
    Ow->>BFF: PUT /api/stores/orders/sold { storeId, orderId }
    BFF->>OS: PUT /api/stores/orders/sold
    OS-->>BFF: OrderResponse (SOLD, items[])
    BFF->>SS: POST /api/stores/find { storeId }
    SS-->>BFF: StoreResponse (name)
    BFF->>SS: POST /api/stores/products/list { storeId }
    SS-->>BFF: List[ProductResponse] (names)
    BFF-->>Ow: OrderResponse (SOLD)
    Note over BFF,NS: async — fire and forget
    BFF-)NS: POST /internal/notifications (ORDER_SOLD → customer, items)
    NS--)Cu: notification visible on next poll

    Ow->>BFF: PUT /api/stores/products/popularity { storeId, productId, delta: 1 }
    BFF-->>Ow: ProductResponse

    Ow->>BFF: POST /api/stores/statistics/revenue { storeId, year, month }
    BFF-->>Ow: RevenueResponse
```

---

## Database Schema

```mermaid
erDiagram
    %% ── userdb :5433 (user-service) ────────────────────────
    users {
        bigserial id PK
        varchar   email
        varchar   password_hash
        varchar   phone
        varchar   role        "CUSTOMER | OWNER | ADMIN"
        varchar   status      "ACTIVE | SUSPENDED | WITHDRAWN"
        bigint    created_at
        bigint    updated_at
    }

    %% ── storedb :5434 (store-service) ──────────────────────
    stores {
        bigserial id PK
        bigint    user_id              "* cross-service ref — no FK"
        varchar   name
        varchar   address
        varchar   phone
        text      content
        varchar   status               "ACTIVE | INACTIVE"
        varchar   store_picture_url    "nullable"
        bigint    product_created_time
        bigint    opened_time
        bigint    closed_time
        varchar   closed_days
        bigint    created_at
        bigint    updated_at
    }
    products {
        bigserial id PK
        bigint    store_id FK
        varchar   name
        text      description
        bigint    price
        varchar   product_picture_url "nullable"
        bigint    popularity
        boolean   status
        bigint    created_at
        bigint    updated_at
    }
    reviews {
        bigserial id PK
        bigint    store_id FK
        bigint    user_id     "* cross-service ref — no FK"
        int       rating
        text      content
        bigint    created_at
        bigint    updated_at
    }

    %% ── orderdb :5435 (order-service) ──────────────────────
    carts {
        bigserial id PK
        bigint    user_id     "* cross-service ref — no FK, indexed non-unique"
        bigint    store_id    "* cross-service ref — no FK"
        boolean   is_ordered
        bigint    created_at
        bigint    updated_at
    }
    cart_products {
        bigserial id PK
        bigint    cart_id FK
        bigint    product_id  "* cross-service ref — no FK"
        bigint    quantity
        bigint    unit_price
    }
    orders {
        bigserial id PK
        bigint    cart_id FK
        bigint    user_id     "* cross-service ref — no FK"
        bigint    store_id    "* cross-service ref — no FK"
        bigint    total_price
        varchar   status      "PENDING | SOLD | CANCELED"
        bigint    created_at
        bigint    updated_at
    }

    %% ── notificationdb :5436 (notification-service) ────────
    notifications {
        bigserial id PK
        bigint    user_id     "* cross-service ref — no FK, indexed non-unique"
        varchar   type        "NEW_ORDER | ORDER_SOLD | ORDER_CANCELED"
        varchar   title
        text      content
        bigint    store_id    "nullable snapshot — no FK"
        varchar   store_name  "nullable snapshot"
        text      items       "JSON array snapshot"
        boolean   is_read
        bigint    issued_at
        bigint    expiry
        bigint    created_at
    }
    public_notifications {
        bigserial id PK
        varchar   title
        text      content
        boolean   is_active   "indexed — list query filters by this"
        bigint    issued_at
        bigint    expires_at
    }

    %% relationships within storedb
    stores      ||--o{ products      : "has"
    stores      ||--o{ reviews       : "has"

    %% relationships within orderdb
    carts       ||--o{ cart_products : "contains"
    carts       ||--o| orders        : "becomes"
```

---

## CI/CD — Jenkins Pipeline

### Pipeline Overview

```mermaid
flowchart LR
    GH["GitHub\n(main branch)"]
    DH["Docker Hub\nprolmpa/*"]
    AC["ArgoCD\n(auto-sync)"]

    subgraph jenkins ["Jenkins Server"]
        CO["1. Checkout"]
        BI["2. Build Images\n./gradlew bootJar\ndocker build x 6"]
        PI["3. Push Images\ndocker push x 6"]
        UH["4. Update Helm values\nvalues.yaml images.tag\ngit commit + push"]
        DP["5. Deploy Monitoring\nenvsubst → SCP → systemctl reload"]
    end

    subgraph k8s ["minikube cluster (baemin namespace)"]
        direction LR
        BFF["bff-service :30080"]
        US["user-service :30081"]
        SS["store-service :30082"]
        OS["order-service :30083"]
        NS["notification-service :30084"]
        FS["front-service :30000"]
    end

    VM["vm-monitoring\nPrometheus · Alertmanager · Grafana"]

    GH -->|push| CO --> BI --> PI --> UH & DP
    PI --> DH
    UH -->|git push| GH
    GH -->|detects change| AC
    AC --> k8s
    DP -->|SSH| VM
```

### Stages

| Stage | What happens |
|---|---|
| **Checkout** | Pulls `main` branch from GitHub |
| **Build Images** | `./gradlew bootJar -x test` — produces fat JARs; `docker build` for all 6 services (5 Spring + `front-service`); each image tagged with a 12-char git SHA and `latest` |
| **Push Images** | `docker login` with `docker-hub-cred`; pushes both tags for all 6 images to Docker Hub; removes local images immediately after push to prevent disk exhaustion |
| **Update Helm values** | Writes the 12-char git SHA into `helm/baemin/values.yaml` (`images.tag`), commits, and pushes to `main`. ArgoCD detects the change automatically via its automated sync policy (`prune: true`, `selfHeal: true`) and reconciles the cluster — no `kubectl apply` runs from Jenkins. |
| **Deploy Monitoring** | `envsubst` substitutes `${MINIKUBE_IP}` and Telegram credentials into config templates, copies via SCP to `/opt/monitoring/` on vm-monitoring, and reloads Prometheus and Alertmanager via `sudo systemctl reload` — no process restart required. |

Stages 4 and 5 run in parallel. All 6 services run as Kubernetes Deployments in the `baemin` namespace (Helm chart at `helm/baemin/`), exposed via NodePort Services. ArgoCD owns all cluster state; Jenkins only pushes image tags to git. Prometheus scrapes metrics from all services via minikube NodePorts.

### Jenkins Credentials & Global Env Vars

Credentials stored in Jenkins — no values appear in the `Jenkinsfile`:

| Credential ID | Type | Used for |
|---|---|---|
| `github-cred` | Username/password | Checkout from GitHub |
| `docker-hub-cred` | Username/password | `docker login` to Docker Hub |
| `deploy-ssh-key` | SSH private key | `sshagent` for SSH/SCP to vm-monitoring |
| `minikube-kubeconfig` | Secret file | `withKubeConfig` — kubeconfig for the minikube cluster |
| `telegram-bot-token` | Secret text | Alertmanager webhook to Telegram |
| `telegram-chat-id` | Secret text | Alertmanager webhook to Telegram |
| `MINIKUBE_IP` | Secret text | `minikube ip` output — Prometheus NodePort scrape targets |

`MONITORING_HOST`, DB connection strings, JWT secret, and HMAC secrets are stored as Jenkins Global Environment Variables. `MONITORING_HOST` is used for SSH to vm-monitoring; the rest are injected into Kubernetes Secrets at deploy time.
