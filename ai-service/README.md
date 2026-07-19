# ai-service

Kotlin + Spring Boot 4 microservice that provides AI-powered features for the Baemin food delivery platform using **Spring AI 2.0** and **Anthropic Claude Haiku**.

- Port: **8085** (ClusterIP inside the cluster, exposed through BFF)
- Model: `claude-haiku-4-5-20251001` with a 55 s read timeout

---

## 1. Architecture

```
Browser / Mobile
      │
      ▼
 front-service (port 30000)
      │
      ▼
 bff-service (port 30080)          ← JWT validation is NOT done here;
      │   ╔══════════════╗            token is simply forwarded
      │   ║ CircuitBreaker║  ai
      │   ║  (50%, 60s)  ║
      └──►║              ║
          ╚══════╤═══════╝
                 │ POST /api/ai/recommend
                 │ POST /api/ai/chat
                 ▼
         ai-service :8085           ← HmacRequestFilter + JwtAuthenticationFilter
              │          │
              │          └──► Anthropic Claude API (HTTPS)
              │
        ┌─────┴──────┐
        ▼             ▼
 store-service    order-service     ← called via typed RestClients
    :8082             :8083            with HMAC signing + trace-id propagation
```

ai-service sits **behind the BFF** and is never reachable from the public internet. It makes outbound calls to two internal backend services (store, order) to gather context before calling Anthropic.

---

## 2. Key Design Decisions

### HMAC + JWT dual-layer auth

Every request to ai-service must pass **two filters** in sequence:

| Filter | What it checks | Rejects with |
|---|---|---|
| `HmacRequestFilter` | `X-Bff-Timestamp` + `X-Bff-Signature` on the request | 401 |
| `JwtAuthenticationFilter` | `Authorization: Bearer <token>` | 401 |

The HMAC layer ensures only the BFF can reach ai-service — raw internet traffic is blocked even if port 8085 were accidentally exposed. The JWT layer identifies the end user so downstream service calls can be made on their behalf.

Both filters are shared from the `common` module. `HmacSigningInterceptor` (also in `common`) is used by the two outbound `RestClient` beans to sign requests headed to store-service and order-service.

### `@RequestScope` JWT propagation

The JWT `Authorization` header arrives at the controller and must flow into `RecommendationService`, `ChatService`, and `BaeminTools` (for tool-calling during chat). Rather than threading it through every method signature, it is stored in a **request-scoped bean**:

```kotlin
@Component
@RequestScope
class AiRequestContext {
    var jwt: String? = null
}
```

The controller writes to it once; every service and tool in the same request reads from it. No Spring Security `SecurityContextHolder` gymnastics needed.

### Recommendation — parallel prefetch + `showPreferencePicker`

`RecommendationService.recommend()` fires two backend calls concurrently before touching Claude:

```
CompletableFuture.supplyAsync { storeClient.listProducts(storeId, token) }
CompletableFuture.supplyAsync { orderClient.getUserOrders(token) }
```

This cuts wall-clock latency when order history is large.

The service decides the recommendation strategy based on what context is available:

| Branch | Condition | `showPreferencePicker` |
|---|---|---|
| A — history | past orders exist | `false` |
| B — preferences | no history, but `categoryPreferences` provided | `true` |
| C — cold start | neither available | `true` |

`showPreferencePicker = true` tells the frontend to render a `FoodPreferencePicker` component so the user can supply preferences on the next call.

### Chat — tool calling + server-side history cap

`ChatService` uses Spring AI's function-calling support via `BaeminTools`, a `@Component` annotated with `@Tool` methods:

| Tool | Description |
|---|---|
| `listStores(sortBy)` | Returns all stores sorted by RATING / LATEST / POPULARITY |
| `getStoreDetails(storeId)` | Returns a single store's info |
| `getProducts(storeId)` | Returns the active menu for a store |

This lets Claude autonomously browse the catalogue to answer questions like *"Which fried-chicken places are popular near me?"* without the frontend needing to know about the tool loop.

History is **capped at the last 10 messages** server-side (`req.messages.takeLast(10)`). The frontend may hold an arbitrarily long conversation, but only the tail is ever sent to Anthropic, keeping token usage bounded.

### Error handling and fallbacks

Both services prefer silent degradation over surfacing Anthropic errors to the user:

- `RecommendationService` wraps the Claude call in `runCatching`; on failure it logs a `WARN` and returns an empty recommendation list.
- JSON parse failures (malformed Claude output) are also caught and logged, returning an empty list. Parsing uses `tools.jackson.databind.ObjectMapper` (Jackson 3.x) via `readTree()` — Spring Boot 4 auto-configures Jackson 3.x, not the legacy `com.fasterxml.jackson` bean.
- `ChatService` returns an empty string `""` when `.content()` is null.
- The BFF wraps both endpoints in a `@CircuitBreaker("ai")` with a 50% failure threshold and 60 s open window.

### Explicit `ChatClient` bean

Spring AI 2.0 auto-configures `ChatClient.Builder` but does **not** register a `ChatClient` bean. `RestClientConfig` bridges the gap:

```kotlin
@Bean
fun chatClient(): ChatClient = chatClientBuilder.build()
```

Both `RecommendationService` and `ChatService` inject `ChatClient` directly. Without this explicit bean definition, startup fails with "No qualifying bean of type `ChatClient`".

### Distributed tracing

`TraceIdInterceptor` is registered on both outbound `RestClient` beans. On every outbound HTTP call it copies the current MDC `traceId` into an `X-Request-Id` header, so the same trace ID flows from ai-service into store-service and order-service logs — visible in Kibana across all three services.

---

## 3. How It Works

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/ai/recommend` | JWT | Menu recommendations for a store |
| POST | `/api/ai/chat` | JWT | Multi-turn conversational assistant |
| GET | `/actuator/health` | none | Kubernetes liveness / readiness probe |
| GET | `/actuator/prometheus` | none | Micrometer metrics scrape |

All request/response bodies use `POST` with JSON, consistent with the rest of the platform.

---

### Recommendation flow

```
Client                BFF                  ai-service           store/order    Anthropic
  │                    │                       │                     │              │
  │  POST /recommend   │                       │                     │              │
  ├──────────────────►│                       │                     │              │
  │                    │  POST /api/ai/recommend                    │              │
  │                    │  + HMAC headers       │                     │              │
  │                    ├──────────────────────►│                     │              │
  │                    │                       │─ HmacRequestFilter  │              │
  │                    │                       │─ JwtAuthFilter      │              │
  │                    │                       │                     │              │
  │                    │                       │  POST /api/stores/products/list    │
  │                    │                       ├────────────────────►│              │
  │                    │                       │  POST /api/users/me/orders  (async)│
  │                    │                       ├────────────────────►│              │
  │                    │                       │◄────────────────────┤              │
  │                    │                       │  (both complete)    │              │
  │                    │                       │                     │              │
  │                    │                       │  prompt(system + user message)     │
  │                    │                       ├───────────────────────────────────►│
  │                    │                       │◄───────────────────────────────────┤
  │                    │                       │  JSON: {recommendations:[...]}     │
  │                    │◄──────────────────────┤                     │              │
  │◄───────────────────┤  RecommendResponse    │                     │              │
  │  {recommendations, │                       │                     │              │
  │   showPreference   │                       │                     │              │
  │   Picker}          │                       │                     │              │
```

---

### Chat flow (with tool calling)

```
Client                BFF                  ai-service           store-service   Anthropic
  │                    │                       │                     │              │
  │  POST /chat        │                       │                     │              │
  ├──────────────────►│                       │                     │              │
  │                    ├──────────────────────►│                     │              │
  │                    │                       │─ HmacRequestFilter  │              │
  │                    │                       │─ JwtAuthFilter      │              │
  │                    │                       │                     │              │
  │                    │                       │  messages.takeLast(10)             │
  │                    │                       │                     │              │
  │                    │                       │  prompt(messages + tools)          │
  │                    │                       ├───────────────────────────────────►│
  │                    │                       │◄───────────────────────────────────┤
  │                    │                       │  tool_use: listStores("RATING")    │
  │                    │                       │                     │              │
  │                    │                       │  POST /api/stores/list             │
  │                    │                       ├────────────────────►│              │
  │                    │                       │◄────────────────────┤              │
  │                    │                       │  tool_result → back to Claude      │
  │                    │                       ├───────────────────────────────────►│
  │                    │                       │◄───────────────────────────────────┤
  │                    │                       │  final text reply   │              │
  │                    │◄──────────────────────┤                     │              │
  │◄───────────────────┤  {reply: "..."}       │                     │              │
```

Tool calls may be chained; Claude can call multiple tools in a single turn before producing a final text reply. Spring AI handles the tool loop automatically.

---

## 4. Configuration

| File | Profile | Purpose |
|---|---|---|
| `application.yml` | all | Port, model, actuator, autoconfigure excludes |
| `backend.yaml` | `backend-dev` / `backend-prod` | store-service and order-service URLs |
| `hmac.yaml` | `hmac-dev` / `hmac-prod` | HMAC secrets for inbound (BFF→ai) and outbound (ai→store/order) signing |
| `jwt.yaml` | `jwt-dev` / `jwt-prod` | JWT signing secret shared with user-service |

The default profile group `dev` activates `backend-dev`, `hmac-dev`, and `jwt-dev`.
In production, `ANTHROPIC_API_KEY` and `JWT_SECRET` are injected as Kubernetes Secret environment variables.

---

## 5. Code Review — AS-IS / TO-BE

### 5-1. Resilience

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 1 | `productsFuture.get()` error handling | `productsFuture.get()` is called bare — a store-service timeout or error throws `ExecutionException` and returns a 500 to the caller | Wrap in `runCatching` (same as `ordersFuture`) so a store-service failure falls back to an empty product list and logs a `WARN`, consistent with the order fetch |
| 2 | Outbound executor | Both `CompletableFuture.supplyAsync` calls run on `ForkJoinPool.commonPool()` — shared with all async JVM tasks; blocking HTTP I/O on the common pool risks thread starvation under load | Supply a dedicated fixed/virtual-thread executor (e.g. `Executors.newVirtualThreadPerTaskExecutor()`) to isolate blocking I/O from the common pool |

---

### 5-2. Prompt Quality

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 3 | Store name in recommendation prompt | `storeName` is always `"Store #${req.storeId}"` — Claude receives a placeholder, not the real store name | Call `storeClient.findStore(req.storeId, token)` (already exists) and use `StoreInfo.name`; can be combined with the parallel prefetch block at no extra latency cost |

---

### 5-3. UX / Contract

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 4 | `showPreferencePicker` semantics | Branch B (preferences provided, no history) returns `showPreferencePicker = true` even though the call was successfully personalized from the submitted preferences — the frontend re-renders the picker after a successful recommendation | Return `false` for Branch B (preferences were used; picker not needed) and `true` only for Branch C (cold start); or add a companion field like `recommendationBasis` (`"history"`, `"preferences"`, `"cold-start"`) to let the frontend decide rendering |

---

### 5-4. Token Budget

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 5 | `max-tokens` for chat | `max-tokens: 1024` applies to both the recommendation endpoint (strict JSON, ~200 tokens) and the chat endpoint (multi-turn, multi-tool-loop responses) | Set a lower `max-tokens` on the recommendation `ChatClient` call (e.g. 512) and raise the chat ceiling (e.g. 4096); Spring AI `ChatOptions` can be overridden per-call via `.options(ChatOptionsBuilder.builder().maxTokens(4096).build())` |

---

### 5-5. Timeout Consistency

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 6 | Wall-clock ceiling vs BFF circuit breaker | Outbound backend read timeout = 30 s; Anthropic read timeout = 55 s → worst-case sequential wall-clock reaches 85 s, which exceeds the BFF circuit breaker open window (60 s) | Ensure backend read timeout + Anthropic read timeout ≤ BFF per-call timeout; consider reducing backend read timeout to ≤ 10 s and adding a BFF hard timeout of 65 s to keep the total bounded |

---

### 5-6. Build Artifact Gap

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 7 | `RecommendationService` compiled output | `RecommendationService.class` is absent from `build/classes/` — the last successful build pre-dates this file; the `tools.jackson.databind.ObjectMapper` import (Jackson 3.x) is untested at the compiled level | Run `./gradlew :ai-service:build` to verify the service compiles cleanly end-to-end; confirm the auto-configured `ObjectMapper` bean satisfies the `tools.jackson` import before deploying |
