# CLAUDE.md

## Git Policy

**Never `git push` without explicit user instruction.**
Commit changes freely, but always stop at commit. The user decides when and what to push.

## Build & Run

```bash
# Build all modules
./gradlew build

# Run individual services (requires their DB to be up first)
./gradlew :user-service:bootRun
./gradlew :store-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun

# Run tests
./gradlew test
./gradlew :user-service:test
./gradlew :store-service:test
./gradlew :store-service:test --tests "store.service.store.StoreServiceTest"
./gradlew :notification-service:test
```

## Database

Four separate PostgreSQL instances are defined in docker-compose:

```bash
docker compose up -d      # start all four databases
docker compose down       # stop all databases
```

| Service               | Container               | Port | DB             | User       | Password   |
|-----------------------|-------------------------|------|----------------|------------|------------|
| user-service          | baemin_user_db          | 5433 | userdb         | user_svc   | userpass   |
| store-service         | baemin_store_db         | 5434 | storedb        | store_svc  | storepass  |
| order-service         | baemin_order_db         | 5435 | orderdb        | order_svc  | orderpass  |
| notification-service  | baemin_notification_db  | 5436 | notificationdb | notif_svc  | notifpass  |

Apply schema manually after adding DDL (never auto-created, `ddl-auto: validate`):
```bash
docker compose exec -T user-db          psql -U user_svc  -d userdb          < user-service/src/main/resources/db/schema.sql
docker compose exec -T store-db         psql -U store_svc -d storedb         < store-service/src/main/resources/db/schema.sql
docker compose exec -T order-db         psql -U order_svc -d orderdb         < order-service/src/main/resources/db/schema.sql
docker compose exec -T notification-db  psql -U notif_svc -d notificationdb  < notification-service/src/main/resources/db/schema.sql
```

---

## Architecture

Kotlin 2.x + Spring Boot 4 **microservices** with a shared `common` library module.

**Subprojects (`settings.gradle.kts`):**

```
common/                ← shared library (JWT filter, UserPrincipal, UserRole, GlobalExceptionHandler, Extensions)
bff-service/           ← port 8080 — BFF gateway (routing, aggregation, JWT forwarding)
user-service/          ← port 8081 — auth, user management
store-service/         ← port 8082 — stores, products, reviews, product statistics
order-service/         ← port 8083 — carts, orders, order statistics
notification-service/  ← port 8084 — per-user notifications (created by BFF, read by frontend via BFF)
```

**Tech stack:** Kotlin 2.x, Spring Boot 4, Spring Security, Spring Data JPA + QueryDSL 5.1, PostgreSQL, jjwt 0.12, JUnit 5

---

## `common` Module

Shared library — **not** a Spring Boot app. Declared as `implementation(project(":common"))` by any service that needs it.

### Key classes

**`UserRole`** (`common/src/main/kotlin/common/security/UserRole.kt`)
```kotlin
enum class UserRole { CUSTOMER, OWNER, ADMIN }
```

**`UserPrincipal`** (`common/src/main/kotlin/common/security/UserPrincipal.kt`)
```kotlin
data class UserPrincipal(val id: Long, val role: UserRole)
```

**`GlobalExceptionHandler`** (`common/src/main/kotlin/common/exception/GlobalExceptionHandler.kt`)

Returns RFC 7807 `ProblemDetail` responses (`spring.mvc.problemdetails.enabled: true`):
- `NotFoundException` → 404 Not Found
- `ForbiddenException` → 403 Forbidden
- `ConflictException` → 409 Conflict
- `IllegalArgumentException` → 400 Bad Request (validation / invalid input)

Each response includes `type` (`https://baemin.com/problems/{slug}`), `instance` (request URI), `status`, `detail`.

**`Extensions.kt`** (`common/src/main/kotlin/common/Extensions.kt`)
```kotlin
fun <T> Optional<T>.orThrow(msg: String): T = orElseThrow { NotFoundException(msg) }
```

---

## Package Layout

### Package structure conventions

**user-service** uses a flat `layer` structure (single domain):
```
user.{layer}
```

**store-service** and **order-service** use `layer/subdomain` (multiple subdomains per service):
```
store.{layer}.{subdomain}
order.{layer}.{subdomain}
```
When adding a new subdomain (e.g. `product`), create files under every layer:
`api/product/`, `dto/product/`, `entity/product/`, `repository/product/`, `service/product/`

---

### bff-service
```
bff-service/src/main/kotlin/bff/
├── BffServiceApplication.kt
├── client/       ← UserClient.kt, StoreClient.kt, OrderClient.kt, NotificationClient.kt  (RestClient wrappers)
├── config/       ← SecurityConfig.kt, RestClientConfig.kt
├── api/          ← controllers that expose aggregated endpoints to the front-service
└── dto/          ← request/response DTOs (mirrors or composes backend DTOs)
```

**No own database.** The BFF holds no state — it only forwards and aggregates.

**JWT forwarding:** The BFF extracts the `Authorization: Bearer <token>` header from the incoming request and passes it unchanged to each backend service call. Backend services validate the token independently using the shared JWT secret.

**Cross-service aggregation pattern:**
```kotlin
// Example: add-to-cart flow that requires price from store-service
fun addToCart(request: AddToCartRequest, token: String): CartResponse {
    val product = storeClient.findProduct(request.storeId, request.productId, token)
    return orderClient.addCartItem(request.copy(unitPrice = product.price), token)
}
```

---

### user-service
```
user-service/src/main/kotlin/user/
├── UserServiceApplication.kt
├── api/          ← UserController.kt
├── config/       ← SecurityConfig.kt
├── dto/          ← UserRequest.kt  (RegisterUserRequest, LoginUserRequest, SuspendUserRequest, RegisterCommand, LoginCommand)
│                   UserResponse.kt (TokenResponse)
├── entity/       ← User.kt (role: common.security.UserRole), UserStatus.kt
├── repository/   ← UserRepository.kt
├── security/     ← JwtProvider.kt (token generation; stays in user-service only)
└── service/      ← UserService.kt
```

### store-service
```
store-service/src/main/kotlin/store/
├── StoreServiceApplication.kt
├── api/
│   ├── store/    ← StoreController.kt
│   ├── product/  ← ProductController.kt, StoreStatisticsController.kt
│   └── review/   ← ReviewController.kt
├── config/       ← SecurityConfig.kt, QueryDslConfig.kt
├── dto/
│   ├── store/    ← StoreRequest.kt  (CreateStoreRequest, UpdateStoreRequest, DeactivateStoreRequest,
│   │                                  CreateStoreCommand, UpdateStoreCommand,
│   │                                  StoreSortBy, ListStoreRequest, FindStoreRequest)
│   │               StoreResponse.kt (StoreInfo, StoreResponse)
│   ├── product/  ← ProductRequest.kt  (CreateProductRequest, UpdateProductRequest,
│   │                                    DeactivateProductRequest, IncrementPopularityRequest,
│   │                                    ListProductRequest, FindProductRequest, PopularProductRequest)
│   │               ProductResponse.kt (ProductInfo, ProductResponse)
│   └── review/   ← ReviewRequest.kt  (CreateReviewRequest, DeleteReviewRequest, ListReviewRequest)
│                   ReviewResponse.kt (ReviewInfo, ReviewResponse)
├── entity/
│   ├── store/    ← Store.kt (userId: Long — no @ManyToOne), StoreStatus.kt
│   └── product/  ← Product.kt
├── repository/
│   ├── store/    ← StoreRepository.kt
│   ├── product/  ← ProductRepository.kt, ProductRepositoryCustom.kt, ProductRepositoryCustomImpl.kt
│   └── review/   ← ReviewRepository.kt, ReviewRepositoryCustom.kt, ReviewRepositoryCustomImpl.kt
└── service/
    ├── store/    ← StoreService.kt
    ├── product/  ← ProductService.kt, ProductStatisticsService.kt
    └── review/   ← ReviewService.kt
```

### order-service
```
order-service/src/main/kotlin/order/
├── OrderServiceApplication.kt
├── api/
│   ├── cart/   ← CartController.kt
│   └── order/  ← OrderController.kt, UserOrderController.kt, StatisticsController.kt,
│                  UserStatisticsController.kt
├── config/     ← SecurityConfig.kt, QueryDslConfig.kt
├── dto/
│   ├── cart/   ← CartRequest.kt  (AddCartItemRequest, RemoveCartItemRequest, ClearCartRequest, CheckoutRequest)
│   │              CartResponse.kt (CartProductResponse, CartResponse)
│   └── order/  ← OrderRequest.kt  (ListOrderRequest, RevenueRequest, SpendingRequest, FindOrderRequest, MarkOrderRequest)
│                  OrderResponse.kt (OrderResponse, RevenueResponse, SpendingResponse)
├── entity/
│   ├── cart/   ← Cart.kt, CartProduct.kt
│   └── order/  ← Order.kt, OrderStatus.kt
├── repository/
│   ├── cart/   ← CartRepository.kt, CartProductRepository.kt
│   └── order/  ← OrderRepository.kt, OrderRepositoryCustom.kt, OrderRepositoryCustomImpl.kt
└── service/
    ├── cart/   ← CartService.kt
    └── order/  ← OrderService.kt, StatisticsService.kt
```

### notification-service
```
notification-service/src/main/kotlin/notification/
├── NotificationServiceApplication.kt
├── api/          ← NotificationController.kt
├── config/       ← SecurityConfig.kt
├── dto/          ← NotificationRequest.kt  (CreateNotificationRequest, MarkReadRequest, ListNotificationRequest)
│                   NotificationResponse.kt (NotificationResponse — no userId field)
├── entity/       ← Notification.kt
├── repository/   ← NotificationRepository.kt
└── service/      ← NotificationService.kt
```

**Note:** `currentUser()`, `UserPrincipal`, `UserRole`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`, `orThrow` are all from `common.*`.

---

## Auth Flow

1. `POST /api/users/signup` → `UserService.register()` → BCrypt hash → returns `{"id": Long}` (201)
2. `POST /api/users/signin` → `UserService.login()` → verify password + status → `JwtProvider.generateToken()` → `TokenResponse(accessToken, tokenType="Bearer")`
3. **All other endpoints** require `Authorization: Bearer <token>`
   - `JwtAuthenticationFilter` parses the JWT and populates `SecurityContextHolder` with `UserPrincipal`

**JWT secret** (same across all services):
```
baemin-jwt-secret-key-must-be-at-least-32-characters-long
```

### FilterRegistrationBean fix (required in every service's SecurityConfig)

Spring Boot auto-registers every `Filter` bean as a standalone servlet filter, causing the JWT filter to run twice. Fix:

```kotlin
@Bean
fun jwtAuthFilterRegistration(jwtAuthFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> =
    FilterRegistrationBean(jwtAuthFilter).apply { isEnabled = false }
```

---

## Service Details

### bff-service (port 8080)

**Role:** Single entry point for all client requests. Routes to the correct backend service and handles cross-service aggregation.

**No own database.** Stateless — forwards requests and aggregates responses only.

**Key responsibilities:**
- **Routing** — delegates each request to the appropriate backend service via `RestClient`
- **Aggregation** — combines responses from multiple services into one (e.g. order list with store/product details)
- **JWT forwarding** — extracts `Authorization: Bearer <token>` from the client request and passes it to every downstream call; each backend service validates independently
- **Cross-service data hand-off** — handles flows where output from one service is input to another (e.g. fetch `unitPrice` from store-service, then call order-service to add a cart item)

**Client wrappers** (`client/` package): one `RestClient`-based class per backend service (`UserClient`, `StoreClient`, `OrderClient`, `NotificationClient`). Each method maps to one backend endpoint and forwards the JWT header. `NotificationClient.createNotification` calls `/internal/notifications` without a JWT (BFF is the trusted caller).

**Does not use `common` security filter** — the BFF is not a resource server. It does not validate JWT tokens itself; it only forwards them.

---

### user-service (port 8081)

**Endpoints:**
```
POST /api/users/signup          → register (public) — returns {"id": Long}
POST /api/users/signin          → login, returns JWT (public)
PUT  /api/users/suspend         → suspend user; body: { id } (ADMIN) — sets status SUSPENDED
PUT  /api/users/me/withdraw     → self-withdraw (any authenticated) — sets status WITHDRAWN
```

**UserRole** (`common.security.UserRole`): `CUSTOMER`, `OWNER`, `ADMIN`

**User status rules:**
- Only `ACTIVE` users can be suspended or withdrawn (throws 409 otherwise)
- `ADMIN` role check for suspend is done in service (throws 409 if not ADMIN)
- Login blocks non-`ACTIVE` accounts

**Schema:** `users` table

---

### store-service (port 8082)

**Domains:** Store, Product, Review

**Store endpoints:**
```
POST   /api/stores                      → create store (OWNER)
POST   /api/stores/list                 → list all stores; body: { sortBy: "CREATED_AT"|"RATING" }
POST   /api/stores/mine                 → owner's own stores (OWNER)
POST   /api/stores/find                 → store detail; body: { id }
PUT    /api/stores                      → update store; body: { id, ...fields } (OWNER, must own)
PUT    /api/stores/deactivate           → soft-delete store; body: { id } — sets status INACTIVE (OWNER, must own)
```

**Store business rules:**
- Only `OWNER` may create; one owner → **many stores** (1:N)
- Soft delete: `deactivate()` sets `StoreStatus.INACTIVE`; no row is ever removed from DB
- Times (`openedTime`, `closedTime`, `productCreatedTime`) stored as `Long` (epoch millis)
- `userId: Long` — plain column, no `@ManyToOne` (cross-DB boundary)
- Ownership check: `store.userId != principal.id`

**Schema:** `stores` table with `user_id BIGINT NOT NULL` + non-unique index `idx_stores_user_id` (no FK constraint)

---

**Product endpoints:**
```
POST   /api/stores/products                         → create product; body: { storeId, ...fields } (OWNER, must own store)
POST   /api/stores/products/list                    → list products; body: { storeId }
POST   /api/stores/products/find                    → product detail; body: { storeId, productId }
PUT    /api/stores/products                         → update product; body: { storeId, productId, ...fields } (OWNER, must own store)
PUT    /api/stores/products/deactivate              → deactivate product; body: { storeId, productId } (OWNER, must own store)
PUT    /api/stores/products/popularity              → increment popularity; body: { storeId, productId, delta } (OWNER, must own store)
POST   /api/stores/statistics/popular-products      → top products by popularity; body: { storeId } (OWNER)
```

**Product business rules:**
- `popularity: Long` starts at 0; incremented by client calling the popularity endpoint after marking an order `SOLD`
- Deactivate: sets `status = false`; no row deleted
- Popular products query uses QueryDSL (`ORDER BY popularity DESC`)

**Schema:** `products` table with `price BIGINT`, `popularity BIGINT`, `status BOOLEAN`

---

**Review endpoints:**
```
POST   /api/stores/reviews                        → create review; body: { storeId, rating, content } (CUSTOMER)
POST   /api/stores/reviews/list                   → list reviews; body: { storeId }
DELETE /api/stores/reviews                        → delete review; body: { storeId, reviewId } (CUSTOMER who wrote it, or ADMIN)
```

**Review business rules:**
- `rating: Int` (1–5); `content: String`
- `userId: Long` — plain column (cross-DB boundary)
- Delete: owner check `review.userId != principal.id` unless `ADMIN`

**Schema:** `reviews` table with `store_id BIGINT NOT NULL REFERENCES stores(id)`, `user_id BIGINT NOT NULL`

---

### order-service (port 8083)

**Domains:** Cart, Order, Statistics

**Cart endpoints:**
```
POST   /api/carts                               → add item; body: { productId, storeId, unitPrice, quantity } (CUSTOMER)
POST   /api/carts/me                            → get caller's active cart (CUSTOMER)
DELETE /api/carts/products                      → remove one item; body: { cartId, productId } (CUSTOMER, must own cart)
DELETE /api/carts                               → clear all items; body: { cartId } (CUSTOMER, must own cart)
PUT    /api/carts/checkout                      → checkout; body: { cartId } → creates Order(PENDING) (CUSTOMER, must own cart)
```

**Cart business rules:**
- A user can have multiple carts; only one is active (`is_ordered = false`). Queried via `findByUserIdAndIsOrderedFalse`.
- Ordered carts (`is_ordered = true`) are preserved as history, permanently linked to their order via `orders.cart_id`.
- Replacing store: active cart is reset in-place (items cleared, `store_id` updated).
- Snapshot: `unit_price` is provided by the client at add-time (already known from the product page).

**Order endpoints:**
```
POST   /api/stores/orders/list                      → list store's orders; body: { storeId } (OWNER)
PUT    /api/stores/orders/sold                      → mark SOLD; body: { storeId, orderId } (OWNER)
PUT    /api/stores/orders/cancel                    → mark CANCELED; body: { storeId, orderId } (OWNER)
POST   /api/users/me/orders                         → caller's order history (CUSTOMER)
```

**Order status flow:** `PENDING → SOLD | CANCELED` (only PENDING can transition)

**Statistics endpoints:**
```
POST   /api/stores/statistics/revenue         → monthly revenue; body: { storeId, year, month, timezone? } (OWNER)
POST   /api/users/me/statistics/spending      → monthly spending; body: { year, month, timezone? } (CUSTOMER)
```

**Schema:** `carts` (`user_id BIGINT NOT NULL` — non-unique, indexed), `cart_products`, `orders` (`total_price BIGINT`) tables

---

### notification-service (port 8084)

**Endpoints:**
```
POST  /internal/notifications        → create notification (no JWT — BFF is trusted caller)
POST  /api/notifications/list        → list caller's notifications; body: { unreadOnly: Boolean } (any auth)
PUT   /api/notifications/read        → mark one read; body: { notificationId } (any auth)
PUT   /api/notifications/read-all    → mark all read (any auth)
```

**Security:** `/internal/**` is `permitAll()`. All `/api/**` require JWT. `userId` comes from `currentUser()` — never from the request body.

**Notification triggers (BFF-side):**
| Event | BFF calls | Recipient |
|---|---|---|
| `checkout` | `storeClient.findStore(storeId)` → owner's `userId` | Store owner |
| `markSold` | `orderClient.markSold(...)` → `order.userId` | Customer |
| `cancelOrder` | `orderClient.cancelOrder(...)` → `order.userId` | Customer |

**userId security:** `StoreResponse.userId` and `OrderResponse.userId` in the BFF are annotated `@JsonProperty(access = WRITE_ONLY)` — deserialized from backends, never serialized to the frontend.

**Notification business rules:**
- `expiry` must be at least 10 minutes after `issuedAt` (enforced in `Notification.init`). BFF sets `expiry = now + 24h`.
- `isRead` starts `false`; set to `true` by `markRead` or `markAllRead`.
- Ownership check in `markRead`: if `notification.userId != currentUser().id` → `ForbiddenException`.

**Schema:** `notifications` table with `user_id BIGINT NOT NULL`, `issued_at BIGINT NOT NULL`, `expiry BIGINT NOT NULL`, `is_read BOOLEAN NOT NULL DEFAULT FALSE`; index `idx_notifications_user_id`.

---

## JPA Notes

- `ddl-auto: validate` — schema must exist before startup; Hibernate never creates tables
- Primary keys: `Long` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`; pass `0L` for new entities
- All entities must be `open class` — required for Hibernate proxy generation (`allOpen` plugin in root `build.gradle.kts`)
- Timestamps: `Long` (epoch millis via `System.currentTimeMillis()`)
- Enums: `@Enumerated(EnumType.STRING)` / `VARCHAR`
- Money and accumulative fields (`price`, `unit_price`, `total_price`, `popularity`, `quantity`, `total_revenue`, `total_spending`): always `Long` / `BIGINT`

---

## Build Configuration Notes

**Each service's `build.gradle.kts`** must include:
- `kotlin("plugin.spring")` — makes Spring beans `open` (CGLIB proxies)
- `kotlin("plugin.jpa")` — no-arg constructors for `@Entity` classes

**`allOpen` block** must include JPA annotations:
```kotlin
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

**QueryDSL** (`store-service`, `order-service`): `kotlin("kapt")` + `querydsl-jpa:5.1.0:jakarta` + `querydsl-apt:5.1.0:jakarta` via `kapt`. Q-classes are generated into `build/generated/source/kapt/main/`. A `QueryDslConfig` bean exposes `JPAQueryFactory`.

**Compiler options** in all subprojects:
```kotlin
freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
```
The `-Xannotation-default-target=param-property` option makes Jakarta validation annotations on constructor parameters work without `@field:` prefix.

---

## Shared Patterns

### HTTP method convention
- **All endpoints** use `POST` / `PUT` / `DELETE` with a JSON request body — **no path variables anywhere**
- Resource IDs are always passed in the request body DTO, never in the URL path

```kotlin
// Create: POST with body containing all fields including parent resource ID
@PostMapping("/api/stores/products")
fun create(@RequestBody request: CreateProductRequest): ProductResponse
// CreateProductRequest(storeId: Long, name: String, ...)

// Read: POST with body — no path variable
@PostMapping("/api/stores/products/find")
fun findById(@RequestBody request: FindProductRequest): ProductResponse
// FindProductRequest(storeId: Long, productId: Long)

// Update: PUT with body containing resource ID + fields
@PutMapping("/api/stores/products")
fun update(@RequestBody request: UpdateProductRequest): ProductResponse
// UpdateProductRequest(storeId: Long, productId: Long, name: String, ...)

// Delete: DELETE with body containing resource ID
@DeleteMapping("/api/stores/reviews")
fun delete(@RequestBody request: DeleteReviewRequest)
// DeleteReviewRequest(storeId: Long, reviewId: Long)
```

### No @RequestMapping on controller class
Write the full path directly on each method annotation:
```kotlin
// CORRECT
@RestController
class StoreController {
    @PostMapping("/api/stores")        fun create(): ...
    @PostMapping("/api/stores/list")   fun listAll(): ...
    @PostMapping("/api/stores/find")   fun findById(): ...
    @PutMapping("/api/stores")         fun update(): ...
}

// WRONG — do not do this
@RestController
@RequestMapping("/api/stores")
class StoreController { ... }
```

### No @Transactional in repository layer
`@Transactional` belongs only in the **service layer**. Repository interfaces (extending `JpaRepository`) and custom `RepositoryCustomImpl` classes must not carry `@Transactional` annotations.

### Service layer must return DTOs, never entities
**Services must never return a persistence entity.** All service methods must return a response DTO (or a primitive/`Unit`). Map entities to DTOs inside the service using the companion `.of()` factory before returning.

```kotlin
// CORRECT — service returns DTO
fun create(...): OrderResponse {
    val entity = repository.save(Order(...))
    return OrderResponse.of(entity)
}

// WRONG — entity leaks out of the service layer
fun create(...): Order {
    return repository.save(Order(...))
}
```

### Only request/response DTOs cross the controller–service boundary
The **only** types that may pass between a controller and a service are request DTOs (input) and response DTOs (output). Intermediate wrapper types that hold raw entities (e.g. a data class containing an `@Entity` field) are forbidden — they are entity leaks in disguise.

```kotlin
// CORRECT
fun addItem(request: AddCartItemRequest, userId: Long): CartResponse  // DTO in, DTO out

// WRONG — wrapper holds entity fields; controller must not touch entities
data class CartInfo(val cart: Cart, val items: List<CartProduct>)     // forbidden
fun addItem(request: AddCartItemRequest, userId: Long): CartInfo
```

### Ownership check (store-service — plain userId column)
```kotlin
if (store.userId != currentUser().id) throw ForbiddenException("Forbidden")
```

### Response DTO factory
```kotlin
data class StoreResponse(...) {
    companion object {
        fun of(store: Store) = StoreResponse(...)
    }
}
```

### DTO file layout
Each domain has exactly two DTO files:
- `{Domain}Request.kt` — all input-side classes (request bodies, commands, query DTOs)
- `{Domain}Response.kt` — all output-side classes (info projections, response wrappers)

---

## Testing Conventions

- **Unit tests**: plain JUnit 5 + `@ExtendWith(MockitoExtension::class)`, no Spring context
- **Slice tests**: `@WebMvcTest` + `@Import(SecurityConfig::class)` + `@MockitoBean` for the service
- Controller tests: send a real JWT in `Authorization: Bearer <token>`; filter parses it to `UserPrincipal`
- POST-body tests: set `.contentType(MediaType.APPLICATION_JSON).content(...)` on every POST with a body
- Mockito 5 + Kotlin: avoid `any()` matchers; stub with exact objects to prevent NPE from `@NonNull` annotation

### Test checklist per domain
- [ ] Service: happy path
- [ ] Service: entity not found → `NotFoundException` (404)
- [ ] Service: wrong owner → `ForbiddenException` (403)
- [ ] Service: invalid state transition → `ConflictException` (409)
- [ ] Controller: 2xx with correct response body
- [ ] Controller: 4xx on bad input
- [ ] Controller: 4xx delegated from service mock

---

## Implementation Status

| Domain       | Service                | Schema | Entity | Service | Controller | Tests |
|--------------|------------------------|:------:|:------:|:-------:|:----------:|:-----:|
| User         | user-service           | ✅     | ✅     | ✅      | ✅         | ✅    |
| Store        | store-service          | ✅     | ✅     | ✅      | ✅         | ✅    |
| Product      | store-service          | ✅     | ✅     | ✅      | ✅         | ✅    |
| Review       | store-service          | ✅     | ✅     | ✅      | ✅         | ✅    |
| Cart         | order-service          | ✅     | ✅     | ✅      | ✅         | ✅    |
| Order        | order-service          | ✅     | ✅     | ✅      | ✅         | ✅    |
| Statistics   | —                      | ✅     | ✅     | ✅      | ✅         | ✅    |
| Notification | notification-service   | ✅     | ✅     | ✅      | ✅         | ✅    |
| BFF          | bff-service            | —      | —      | ✅      | ✅         | ✅    |

---

## File Checklist per New Domain

For a new subdomain `{sub}` in a multi-domain service (e.g. store-service):

```
1. {service}/src/main/resources/db/schema.sql        ← ADD SQL block
2. entity/{sub}/XxxStatus.kt                          ← enum (if needed)
3. entity/{sub}/Xxx.kt                                ← @Entity open class
4. repository/{sub}/XxxRepository.kt                  ← JpaRepository (no @Transactional)
5. dto/{sub}/XxxRequest.kt                            ← all input-side classes
6. dto/{sub}/XxxResponse.kt                           ← all output-side classes with companion .of()
7. service/{sub}/XxxService.kt                        ← business logic (@Transactional here)
8. api/{sub}/XxxController.kt                         ← REST endpoints (POST for reads, no @RequestMapping on class)
9. test/service/{sub}/XxxServiceTest.kt
10. test/api/{sub}/XxxControllerTest.kt
```

For user-service (single domain), omit the `{sub}/` level:
```
entity/Xxx.kt, repository/XxxRepository.kt, dto/XxxRequest.kt, dto/XxxResponse.kt ...
```
