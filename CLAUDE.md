# CLAUDE.md

## Git Policy

**Never `git push` without explicit user instruction.**
Commit changes freely, but always stop at commit. The user decides when and what to push.

## JDK

**System JDK: 25.** `jvmToolchain(25)` in `build.gradle.kts`. Kotlin 2.3.21.

**Kotlin 2.3.21 + Spring Boot BOM workaround (applied in `build.gradle.kts`):**
Two fixes work around a Kotlin 2.3.21 POM bug: `extra["kotlin.version"] = "2.3.21"` in `subprojects {}` (prevents BOM from downgrading kotlin-compiler-embeddable), and `ComponentMetadataRule AddKotlinBuildStatistics` (patches missing `kotlin-build-statistics:2.3.21` transitive dep). Remove both if a future Kotlin release fixes the `kotlin-build-tools-impl` POM.

## Build & Run

```bash
./gradlew build
./gradlew :user-service:bootRun
./gradlew :store-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun
./gradlew test
./gradlew :store-service:test --tests "store.service.store.StoreServiceTest"
```

## Database

Four PostgreSQL instances run natively on dedicated VMs. Redis is shared at `192.168.160.104:6379`.

| Service              | VM IP           | Port | DB             | User      | Password   |
|----------------------|-----------------|------|----------------|-----------|------------|
| user-service         | 192.168.160.101 | 5432 | userdb         | user_svc  | userpass   |
| store-service        | 192.168.160.102 | 5432 | storedb        | store_svc | storepass  |
| order-service        | 192.168.160.103 | 5432 | orderdb        | order_svc | orderpass  |
| notification-service | 192.168.160.104 | 5432 | notificationdb | notif_svc | notifpass  |

Apply schema after adding DDL (`ddl-auto: validate` — never auto-created):
```bash
psql -h 192.168.160.101 -U user_svc  -d userdb         < user-service/src/main/resources/db/schema.sql
psql -h 192.168.160.102 -U store_svc -d storedb        < store-service/src/main/resources/db/schema.sql
psql -h 192.168.160.103 -U order_svc -d orderdb        < order-service/src/main/resources/db/schema.sql
psql -h 192.168.160.104 -U notif_svc -d notificationdb < notification-service/src/main/resources/db/schema.sql
```

---

## Architecture

Kotlin 2.3.21 + Spring Boot 4 microservices with a shared `common` library module.

```
common/                ← shared library (JWT filter, UserPrincipal, UserRole, GlobalExceptionHandler, Extensions)
front-service/         ← port 30000 (NodePort) — client frontend
bff-service/           ← port 30080 (NodePort) — BFF gateway (routing, aggregation, JWT forwarding)
user-service/          ← port 8081 — auth, user management
store-service/         ← port 8082 — stores, products, reviews, product statistics
order-service/         ← port 8083 — carts, orders, order statistics
notification-service/  ← port 8084 — per-user notifications + public notifications
```

Helm chart at `helm/baemin/` — deployed via ArgoCD GitOps (auto-syncs on `main` push). `values.yaml` holds `images.tag` (CI-managed), `replicas`, `javaOpts`.

**Tech stack:** Kotlin 2.3.21, Spring Boot 4, Spring Security, Spring Data JPA + QueryDSL 5.1, PostgreSQL, jjwt 0.12, Resilience4j 2.3, Spring Boot Actuator + Micrometer, JUnit 5, Kubernetes (minikube), Helm, ArgoCD

---

## `common` Module

Shared library — not a Spring Boot app. `implementation(project(":common"))` in each service.

- **`UserRole`**: `enum class UserRole { CUSTOMER, OWNER, ADMIN }`
- **`UserPrincipal`**: `data class UserPrincipal(val id: Long, val role: UserRole)`
- **`GlobalExceptionHandler`**: RFC 7807 `ProblemDetail` — `NotFoundException`→404, `ForbiddenException`→403, `ConflictException`→409, `IllegalArgumentException`→400
- **`Extensions.kt`**: `fun <T> Optional<T>.orThrow(msg: String)` → `NotFoundException`
- **`HmacRequestFilter`**: validates `X-Bff-Timestamp` + `X-Bff-Signature` before Spring Security; rejects non-BFF callers with `401`. `/actuator/**` exempt.

---

## Package Layout

**user-service** — flat: `user.{layer}` (api, config, dto, entity, repository, security, service)

**store-service / order-service** — layered by subdomain: `store.{layer}.{subdomain}`
When adding a subdomain, create files under every layer: `api/{sub}/`, `dto/{sub}/`, `entity/{sub}/`, `repository/{sub}/`, `service/{sub}/`

Each domain has exactly **two DTO files**: `{Domain}Request.kt` (all input classes) and `{Domain}Response.kt` (all output classes with companion `.of()`).

**bff-service:** `client/` (RestClient wrappers), `config/`, `api/`, `dto/`
**notification-service:** flat — `api/`, `config/`, `dto/`, `entity/`, `repository/`, `service/`

---

## Auth Flow

1. `POST /api/users/signup` → BCrypt hash → `{"id": Long}` (201)
2. `POST /api/users/signin` → verify password + ACTIVE status → JWT → `TokenResponse`
3. All other endpoints require `Authorization: Bearer <token>` — `JwtAuthenticationFilter` populates `SecurityContextHolder` with `UserPrincipal`

**FilterRegistrationBean fix** (every service's SecurityConfig — prevents JWT filter running twice):
```kotlin
@Bean
fun jwtAuthFilterRegistration(jwtAuthFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> =
    FilterRegistrationBean(jwtAuthFilter).apply { isEnabled = false }
```

---

## Service Details

### bff-service (port 8080)
Stateless gateway. No own database. Does **not** validate JWTs — only forwards them. One `RestClient` wrapper per backend (`UserClient`, `StoreClient`, `OrderClient`, `NotificationClient`). All four `NotificationClient` methods carry `@CircuitBreaker(name = "notification")` (COUNT_BASED, 50% threshold, 30 s wait). Notification calls are fire-and-forget (`CompletableFuture.runAsync`); failures are silent.

### user-service (port 8081)
`UserRole`: `CUSTOMER`, `OWNER`, `ADMIN`. Only `ACTIVE` users can sign in, be suspended, or withdraw — throws `ConflictException` otherwise. ADMIN role check for suspend is in the service layer.

### store-service (port 8082)
**Store:** OWNER-only create; 1:N owner→stores. Soft-delete sets `StoreStatus.INACTIVE`. `userId: Long` — no `@ManyToOne`. Ownership: `store.userId != principal.id`.
**Product:** `popularity: Long` incremented post-SOLD via dedicated endpoint. Deactivate sets `status = false`. Popular-products: QueryDSL `ORDER BY popularity DESC`.
**Review:** `rating: Int` (1–5). Delete allowed by owner or ADMIN.
**Cache:** `stores`, `stores-all`, `products`, `products-by-store` cached in Redis via `@Cacheable` / `@CacheEvict`.

### order-service (port 8083)
**Cart:** One active cart per user (`is_ordered = false`). Adding from a different store resets the active cart in-place. Ordered carts preserved as history. `unit_price` snapshotted at add-time.
**Order:** `PENDING → SOLD | CANCELED` (only PENDING can transition). Transitions owned by OWNER.
**Cache:** `orders-by-user` cached in Redis via `@Cacheable` / `@CacheEvict`.

### notification-service (port 8084)
Two entity types: `notifications` (per-user, JWT-gated) and `public_notifications` (system-wide, unauthenticated).

**Per-user notifications:** `/internal/notifications` is `permitAll()` — BFF trusted caller, no JWT. All `/api/**` require JWT. `userId` always from `currentUser()`, never from request body. `expiry` must be ≥ 10 min after `issuedAt`. Ownership enforced in `markRead`.

**Public notifications:** `POST /api/public-notifications/list` — unauthenticated, highest-frequency read endpoint. Uses a **two-level cache**: Caffeine L1 (in-process, TTL 1 min) → Redis L2 (shared, TTL 10 min) → PostgreSQL. Both cache layers are evicted atomically on create/deactivate. Cache key: `public-notifications:active` (entire active list as single JSON array).

---

## JPA Notes

- `open-in-view: false` — disabled in all backend services
- `ddl-auto: validate` — schema must exist before startup; Hibernate never creates tables
- Primary keys: `Long` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`; pass `0L` for new entities
- All entities must be `open class` (Hibernate proxy via `allOpen` plugin)
- Timestamps: `Long` (epoch millis via `System.currentTimeMillis()`)
- Enums: `@Enumerated(EnumType.STRING)` / `VARCHAR`
- Money/accumulative fields (`price`, `total_price`, `popularity`, etc.): always `Long` / `BIGINT`

---

## Build Configuration Notes

Each service's `build.gradle.kts` must include `kotlin("plugin.spring")` and `kotlin("plugin.jpa")`.

`allOpen` block must list `jakarta.persistence.Entity`, `MappedSuperclass`, `Embeddable`.

**QueryDSL** (store-service, order-service): `kotlin("kapt")` + `querydsl-jpa:5.1.0:jakarta` + `querydsl-apt:5.1.0:jakarta`. Q-classes generated into `build/generated/source/kapt/main/`. `QueryDslConfig` bean exposes `JPAQueryFactory`.

Compiler options (all subprojects):
```kotlin
freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
```
`-Xannotation-default-target=param-property` makes Jakarta validation annotations work on constructor parameters without `@field:` prefix.

---

## Shared Patterns

### HTTP method convention
All endpoints use `POST` / `PUT` / `DELETE` with a JSON body — **no path variables anywhere**. Resource IDs go in the request body DTO, not the URL. Read operations use `POST /api/stores/find` with `{ id }`, not `GET /api/stores/{id}`.

### No @RequestMapping on controller class
Write the full path on each method annotation directly. Never put `@RequestMapping` on the class.

### No @Transactional in repository layer
`@Transactional` belongs only in the service layer. `JpaRepository` interfaces and `RepositoryCustomImpl` classes must not carry it.

### Service layer must return DTOs, never entities
All service methods return a response DTO (or `Unit`). Map entities to DTOs inside the service using the companion `.of()` factory. Never return an `@Entity` from a service method.

### Ownership check (cross-service userId column)
```kotlin
if (store.userId != currentUser().id) throw ForbiddenException("Forbidden")
```

### Response DTO factory
```kotlin
data class StoreResponse(...) {
    companion object { fun of(store: Store) = StoreResponse(...) }
}
```

---

## Implementation Status

| Domain       | Service              | Schema | Entity | Service | Controller | Tests |
|--------------|----------------------|:------:|:------:|:-------:|:----------:|:-----:|
| User         | user-service         | ✅     | ✅     | ✅      | ✅         | ✅    |
| Store        | store-service        | ✅     | ✅     | ✅      | ✅         | ✅    |
| Product      | store-service        | ✅     | ✅     | ✅      | ✅         | ✅    |
| Review       | store-service        | ✅     | ✅     | ✅      | ✅         | ✅    |
| Cart         | order-service        | ✅     | ✅     | ✅      | ✅         | ✅    |
| Order        | order-service        | ✅     | ✅     | ✅      | ✅         | ✅    |
| Statistics   | —                    | ✅     | ✅     | ✅      | ✅         | ✅    |
| Notification | notification-service | ✅     | ✅     | ✅      | ✅         | ✅    |
| BFF          | bff-service          | —      | —      | ✅      | ✅         | ✅    |
