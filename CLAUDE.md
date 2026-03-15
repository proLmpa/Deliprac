# CLAUDE.md

## Build & Run

```bash
# Build all modules
./gradlew build

# Run individual services (requires their DB to be up first)
./gradlew :user-service:bootRun
./gradlew :store-service:bootRun
./gradlew :order-service:bootRun

# Run tests
./gradlew test
./gradlew :user-service:test
./gradlew :store-service:test
./gradlew :store-service:test --tests "store.service.store.StoreServiceTest"
```

## Database

Three separate PostgreSQL instances are defined in docker-compose:

```bash
docker compose up -d      # start all three databases
docker compose down       # stop all databases
```

| Service       | Container          | Port | DB       | User      | Password   |
|---------------|--------------------|------|----------|-----------|------------|
| user-service  | baemin_user_db     | 5433 | userdb   | user_svc  | userpass   |
| store-service | baemin_store_db    | 5434 | storedb  | store_svc | storepass  |
| order-service | baemin_order_db    | 5435 | orderdb  | order_svc | orderpass  |

Apply schema manually after adding DDL (never auto-created, `ddl-auto: validate`):
```bash
docker compose exec -T user-db  psql -U user_svc  -d userdb  < user-service/src/main/resources/db/schema.sql
docker compose exec -T store-db psql -U store_svc -d storedb < store-service/src/main/resources/db/schema.sql
docker compose exec -T order-db psql -U order_svc -d orderdb < order-service/src/main/resources/db/schema.sql
```

---

## Architecture

Kotlin 2.x + Spring Boot 4 **microservices** with a shared `common` library module.

**Subprojects (`settings.gradle.kts`):**

```
common/         ← shared library (JWT filter, UserPrincipal, UserRole, GlobalExceptionHandler, Extensions)
user-service/   ← port 8081 — auth, user management
store-service/  ← port 8082 — stores, products, reviews, product statistics
order-service/  ← port 8083 — carts, orders, order statistics
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
data class UserPrincipal(val id: Long, val email: String, val role: UserRole)
```

**`GlobalExceptionHandler`** (`common/src/main/kotlin/common/exception/GlobalExceptionHandler.kt`)
- `IllegalArgumentException` → 400 Bad Request
- `IllegalStateException` → 409 Conflict

**`Extensions.kt`** (`common/src/main/kotlin/common/Extensions.kt`)
```kotlin
fun <T> Optional<T>.orThrow(msg: String): T = orElseThrow { IllegalArgumentException(msg) }
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

### user-service
```
user-service/src/main/kotlin/user/
├── UserServiceApplication.kt
├── api/          ← UserController.kt
├── config/       ← SecurityConfig.kt
├── dto/          ← RegisterUserRequest, LoginUserRequest, TokenResponse, RegisterCommand, LoginCommand
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
│   ├── product/  ← ProductController.kt, InternalProductController.kt, StoreStatisticsController.kt
│   └── review/   ← ReviewController.kt
├── config/       ← SecurityConfig.kt, QueryDslConfig.kt
├── dto/
│   ├── store/    ← CreateStoreRequest.kt, UpdateStoreRequest.kt, StoreInfo.kt, StoreResponse.kt
│   │                CreateStoreCommand.kt, UpdateStoreCommand.kt
│   ├── product/  ← CreateProductRequest.kt, UpdateProductRequest.kt, ProductInfo.kt, ProductResponse.kt
│   └── review/   ← CreateReviewRequest.kt, ReviewInfo.kt, ReviewResponse.kt
├── entity/
│   ├── store/    ← Store.kt (userId: Long — no @ManyToOne), StoreStatus.kt
│   └── product/  ← Product.kt
├── repository/
│   ├── store/    ← StoreRepository.kt (findByUserId: List<Store>)
│   ├── product/  ← ProductRepository.kt, ProductRepositoryCustom.kt, ProductRepositoryCustomImpl.kt
│   └── review/   ← ReviewRepository.kt
└── service/
    ├── store/    ← StoreService.kt
    ├── product/  ← ProductService.kt, ProductStatisticsService.kt
    └── review/   ← ReviewService.kt
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

Spring Boot auto-registers every `Filter` bean as a standalone servlet filter, causing the JWT filter to run twice (once standalone, once inside `FilterChainProxy`). Fix:

```kotlin
@Bean
fun jwtAuthFilterRegistration(jwtAuthFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> =
    FilterRegistrationBean(jwtAuthFilter).apply { isEnabled = false }
```

---

## Service Details

### user-service (port 8081)

**Endpoints:**
```
POST /api/users/signup          → register (public) — returns {"id": Long}
POST /api/users/signin          → login, returns JWT (public)
PUT  /api/users/{id}/suspend    → suspend user (ADMIN) — sets status SUSPENDED
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
POST   /api/stores                     → create store (OWNER)
GET    /api/stores                     → list all stores (public)
GET    /api/stores/mine                → owner's own stores (OWNER) — List<StoreResponse>
GET    /api/stores/{id}                → store detail (public)
PUT    /api/stores/{id}                → update store (OWNER, must own)
PUT    /api/stores/{id}/deactivate     → soft-delete store — sets status INACTIVE (OWNER, must own)
```

**Store business rules:**
- Only `OWNER` may create; one owner → **many stores** (1:N)
- Soft delete: `deactivate()` sets `StoreStatus.INACTIVE`; no row is ever removed from DB
- Times (`openedTime`, `closedTime`, `productCreatedTime`) stored as `Long` (epoch millis)
- `userId: Long` — plain column, no `@ManyToOne` (cross-DB boundary)
- Ownership check: `store.userId != principal.id`

**Schema:** `stores` table with `user_id BIGINT NOT NULL` + non-unique index `idx_stores_user_id` (no FK constraint — different DB from users)

---

**Product endpoints:**
```
POST   /api/stores/{storeId}/products/{productId}            → create product (OWNER, must own store)
GET    /api/stores/{storeId}/products                        → list products (public)
GET    /api/stores/{storeId}/products/{productId}            → product detail (public)
PUT    /api/stores/{storeId}/products/{productId}            → update product (OWNER, must own store)
PUT    /api/stores/{storeId}/products/{productId}/deactivate → deactivate product (OWNER, must own store)
GET    /api/stores/{storeId}/statistics/popular-products     → top products by popularity (OWNER)
GET    /internal/products/{productId}                        → product info (internal, no auth)
PUT    /internal/products/{productId}/popularity?delta=      → increment popularity (internal, no auth)
```

**Product business rules:**
- `popularity: Int` starts at 0; incremented by order-service via `/internal` endpoint when an order is marked `SOLD`
- Deactivate: sets `active = false`; no row deleted
- Popular products query uses QueryDSL (`ORDER BY popularity DESC`)

**Schema:** `products` table with `store_id BIGINT NOT NULL REFERENCES stores(id)`, `active BOOLEAN`, `popularity INT`

---

**Review endpoints:**
```
POST   /api/stores/{storeId}/reviews              → create review (CUSTOMER)
GET    /api/stores/{storeId}/reviews              → list reviews (any authenticated)
DELETE /api/stores/{storeId}/reviews/{reviewId}   → delete review (CUSTOMER who wrote it, or ADMIN)
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
POST   /api/carts                              → add item to cart (CUSTOMER); creates cart if none; clears if different store
GET    /api/carts                              → get caller's active cart (CUSTOMER)
DELETE /api/carts/{cartId}/products/{productId} → remove one item (CUSTOMER, must own cart)
DELETE /api/carts/{cartId}                    → clear all items (CUSTOMER, must own cart)
PUT    /api/carts/{cartId}/checkout            → checkout → creates Order(PENDING) (CUSTOMER, must own cart)
```

**Cart business rules:**
- `carts.user_id` is UNIQUE. Replacing store: cart is reset in-place (items cleared, `store_id` updated, `is_ordered = false`)
- Snapshot: `unit_price` is copied from product at add-time via `/internal/products/{id}`

**Order endpoints:**
```
GET    /api/stores/{storeId}/orders                     → list store's orders (OWNER, must own store)
PUT    /api/stores/{storeId}/orders/{orderId}/sold      → mark SOLD; increments product popularity (OWNER)
PUT    /api/stores/{storeId}/orders/{orderId}/cancel    → mark CANCELED (OWNER)
GET    /api/users/me/orders                             → caller's order history (CUSTOMER)
GET    /internal/orders/{orderId}                       → order detail (internal, no auth)
```

**Order status flow:** `PENDING → SOLD | CANCELED` (only PENDING can transition)

**Statistics endpoints:**
```
GET    /api/stores/{storeId}/statistics/revenue?year=&month=   → monthly revenue from SOLD orders (OWNER)
GET    /api/users/me/statistics/spending?year=&month=          → monthly spending from SOLD orders (CUSTOMER)
```

**Cross-service calls (RestClient → store-service):**
- `GET  /internal/products/{id}` — validate product active + snapshot price at cart-add time
- `PUT  /internal/products/{id}/popularity?delta=N` — on order marked SOLD

**Schema:** `carts`, `cart_products`, `orders` tables

---

## JPA Notes

- `ddl-auto: validate` — schema must exist before startup; Hibernate never creates tables
- Primary keys: `Long` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`; pass `0L` for new entities
- All entities must be `open class` — required for Hibernate proxy generation (`allOpen` plugin in root `build.gradle.kts`)
- Timestamps: `Long` (epoch millis via `System.currentTimeMillis()`)
- Enums: `@Enumerated(EnumType.STRING)` / `VARCHAR`

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

### Controller URL mapping
Never use `@RequestMapping` on a controller class. Write the full path directly on each HTTP method annotation:
```kotlin
// CORRECT
@RestController
class StoreController {
    @GetMapping("/api/stores")
    fun listAll(): ...

    @PostMapping("/api/stores")
    fun create(): ...

    @GetMapping("/api/stores/{id}")
    fun findById(@PathVariable id: Long): ...
}

// WRONG — do not do this
@RestController
@RequestMapping("/api/stores")
class StoreController {
    @GetMapping fun listAll(): ...
}
```

### Ownership check (store-service — plain userId column)
```kotlin
if (store.userId != currentUser().id) throw IllegalStateException("Forbidden")
```

### Response DTO factory
```kotlin
data class StoreResponse(...) {
    companion object {
        fun of(store: Store) = StoreResponse(...)
    }
}
```

---

## Testing Conventions

- **Unit tests**: plain JUnit 5 + `@ExtendWith(MockitoExtension::class)`, no Spring context
- **Slice tests**: `@WebMvcTest` + `@Import(SecurityConfig::class)` + `@MockitoBean` for the service
- Controller tests: send a real JWT in `Authorization: Bearer <token>`; filter parses it to `UserPrincipal`
- Mockito 5 + Kotlin: avoid `any()` matchers; stub with exact objects to prevent NPE from `@NonNull` annotation

### Test checklist per domain
- [ ] Service: happy path
- [ ] Service: entity not found → `IllegalArgumentException`
- [ ] Service: wrong owner → `IllegalStateException`
- [ ] Service: invalid state transition
- [ ] Controller: 2xx with correct response body
- [ ] Controller: 4xx on bad input
- [ ] Controller: 4xx delegated from service mock

---

## Implementation Status

| Domain     | Service       | Schema | Entity | Service | Controller | Tests |
|------------|---------------|:------:|:------:|:-------:|:----------:|:-----:|
| User       | user-service  | ✅     | ✅     | ✅      | ✅         | ✅    |
| Store      | store-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| Product    | store-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| Review     | store-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| Cart       | order-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| Order      | order-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| Statistics | —             | ✅     | ✅     | ✅      | ✅         | ✅    |

---

## File Checklist per New Domain

For a new subdomain `{sub}` in a multi-domain service (e.g. store-service):

```
1. {service}/src/main/resources/db/schema.sql        ← ADD SQL block
2. entity/{sub}/XxxStatus.kt                          ← enum (if needed)
3. entity/{sub}/Xxx.kt                                ← @Entity open class
4. repository/{sub}/XxxRepository.kt                  ← JpaRepository
5. dto/{sub}/CreateXxxRequest.kt                      ← input DTO
6. dto/{sub}/XxxResponse.kt                           ← output DTO with companion .of()
7. service/{sub}/XxxService.kt                        ← business logic
8. api/{sub}/XxxController.kt                         ← REST endpoints
9. test/service/{sub}/XxxServiceTest.kt
10. test/api/{sub}/XxxControllerTest.kt
```

For user-service (single domain), omit the `{sub}/` level:
```
entity/Xxx.kt, repository/XxxRepository.kt, dto/XxxResponse.kt ...
```
