# CLAUDE(FRONT).md

## Build & Run

```bash
cd front-service
npm install
npm run dev     # http://localhost:5173
npm run build   # production build (runs tsc -b first)
```

All backends must be running before the frontend is usable:

```bash
docker compose up -d
./gradlew :user-service:bootRun &
./gradlew :store-service:bootRun &
./gradlew :order-service:bootRun &
```

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Vite + React + TypeScript | Vite 7, React 19 | App scaffolding |
| Tailwind CSS | v3 | Styling |
| React Router | v6 | SPA routing + role guards |
| Axios | latest | HTTP client with JWT interceptor |
| Zustand | latest | Auth state (token, role, userId) |
| TanStack Query | v5 | Server-state fetching + caching |
| date-fns | latest | Epoch millis → human-readable dates |

---

## Directory Structure

```
front-service/
├── index.html
├── package.json
├── tailwind.config.js
├── vite.config.ts
└── src/
    ├── main.tsx               ← QueryClientProvider + StrictMode
    ├── App.tsx                ← BrowserRouter + Routes + RequireAuth guard
    ├── api/
    │   ├── client.ts          ← axios instance + JWT request interceptor + 401/403 logout
    │   ├── auth.ts            ← signup, signin
    │   ├── stores.ts          ← listStores, getStore, listMyStores, createStore, updateStore,
    │   │                         deactivateStore, getPopularProducts
    │   ├── products.ts        ← listProducts, getProduct, createProduct, updateProduct,
    │   │                         deactivateProduct
    │   ├── reviews.ts         ← listReviews, createReview, deleteReview
    │   ├── cart.ts            ← getCart, addToCart, removeCartItem, clearCart, checkout
    │   └── orders.ts          ← listStoreOrders, markSold, cancelOrder, listMyOrders,
    │                             getRevenue, getSpending
    ├── store/
    │   └── auth.ts            ← Zustand (persisted): { token, userId, role, login(), logout() }
    ├── components/
    │   ├── layout/
    │   │   ├── Header.tsx     ← nav bar with role-based links + logout button
    │   │   └── Layout.tsx     ← Header + <Outlet />
    │   └── ui/
    │       ├── Button.tsx     ← variant: primary | secondary | danger | ghost
    │       ├── Card.tsx
    │       ├── Modal.tsx      ← overlay modal with confirm/cancel
    │       ├── StarRating.tsx ← interactive or readonly star display
    │       └── StatusBadge.tsx ← ACTIVE | INACTIVE | PENDING | SOLD | CANCELED
    └── pages/
        ├── auth/
        │   ├── SignIn.tsx
        │   └── SignUp.tsx      ← radio: CUSTOMER / OWNER
        ├── customer/
        │   ├── StoreList.tsx       ← /stores
        │   ├── StoreDetail.tsx     ← /stores/:id
        │   ├── Cart.tsx            ← /cart
        │   ├── OrderHistory.tsx    ← /orders
        │   └── SpendingStats.tsx   ← /statistics/spending
        └── owner/
            ├── MyStores.tsx        ← /owner/stores
            ├── StoreForm.tsx       ← /owner/stores/new + /owner/stores/:id/edit
            ├── ProductList.tsx     ← /owner/stores/:id/products
            ├── IncomingOrders.tsx  ← /owner/stores/:id/orders
            └── RevenueStats.tsx    ← /owner/stores/:id/revenue
```

---

## Vite Proxy Config

Rules are matched in order — more specific paths must come first.

```typescript
// vite.config.ts
proxy: {
  '/api/users/me/orders':              → http://localhost:8083  (order-service)
  '/api/users/me/statistics':          → http://localhost:8083  (order-service)
  '/api/users':                        → http://localhost:8081  (user-service)
  '/api/carts':                        → http://localhost:8083  (order-service)
  '^/api/stores/\\d+/orders':          → http://localhost:8083  (order-service)  ← regex key
  '^/api/stores/\\d+/statistics/revenue': → http://localhost:8083               ← regex key
  '/api/stores':                       → http://localhost:8082  (store-service)
}
```

**Important:** `/api/users/me/*` must be declared before `/api/users` or they will be caught by the user-service proxy. Regex keys use computed property syntax `['^/api/stores/\\d+/orders']`.

---

## Routing & Role Guards

```
/signin                          → SignIn          (public)
/signup                          → SignUp          (public)
/stores                          → StoreList       (public)
/stores/:id                      → StoreDetail     (public; reviews section requires auth)
/cart                            → Cart            (CUSTOMER only)
/orders                          → OrderHistory    (CUSTOMER only)
/statistics/spending             → SpendingStats   (CUSTOMER only)
/owner/stores                    → MyStores        (OWNER only)
/owner/stores/new                → StoreForm       (OWNER only)
/owner/stores/:id/edit           → StoreForm       (OWNER only)
/owner/stores/:id/products       → ProductList     (OWNER only)
/owner/stores/:id/orders         → IncomingOrders  (OWNER only)
/owner/stores/:id/revenue        → RevenueStats    (OWNER only)
/                                → redirect /stores
*                                → redirect /stores
```

`RequireAuth` component in `App.tsx`:
- No token → redirect to `/signin`
- Wrong role → redirect to `/stores`

---

## Auth Store (Zustand)

```typescript
// src/store/auth.ts
{ token, userId, role }   ← persisted in localStorage as 'baemin-auth'

login(token: string)      ← decodes JWT payload (base64), sets token/userId/role
logout()                  ← clears all fields
```

JWT payload expected fields: `id` (or `sub`) and `role`.

---

## Axios Client

```typescript
// src/api/client.ts
baseURL: '/'   // relative — Vite proxy routes to correct backend

// Request interceptor: Authorization: Bearer <token>
// Response interceptor: 401 or 403 → logout() + redirect /signin
```

---

## Actual Backend Response Shapes

> **These differ from what was initially assumed.** Always verify against the Kotlin DTOs.

### `ProductResponse` (`store-service`)

```typescript
{
  id: number
  storeId: number
  name: string
  description: string
  price: number
  status: boolean        // ← active/inactive flag; NOT named 'active'
  popularity: number
  productPictureUrl: string | null
  createdAt: number
  updatedAt: number
}
```

**Filter active products:** `products.filter(p => p.status)`

### `CartProductResponse` (`order-service`)

```typescript
{
  id: number
  productId: number
  quantity: number
  unitPrice: number
  // NO productName — resolve via listProducts(cart.storeId)
}
```

### `CartResponse` (`order-service`)

```typescript
{
  id: number
  storeId: number
  items: CartItem[]
  totalPrice: number
}
```

### `OrderResponse` (`order-service`)

```typescript
{
  id: number
  storeId: number
  totalPrice: number
  status: 'PENDING' | 'SOLD' | 'CANCELED'
  createdAt: number
  updatedAt: number
  // NO items array — orders do not store line items
  // NO storeName
}
```

---

## Known Patterns & Pitfalls

### Product name in Cart
`CartProductResponse` does not include product names. `Cart.tsx` resolves names by fetching
`listProducts(cart.storeId)` in a second query (enabled only after cart loads) and building a
`productId → name` lookup map:

```typescript
const { data: products } = useQuery({
  queryKey: ['products', cart?.storeId],
  queryFn: () => listProducts(cart!.storeId),
  enabled: !!cart?.storeId,
})
const productNameMap = Object.fromEntries((products ?? []).map(p => [p.id, p.name]))
```

### Time input ↔ epoch millis
Store open/close times are stored as epoch millis. `StoreForm.tsx` converts between
`<input type="time">` strings and epoch millis:

```typescript
// time string → epoch millis (using today's date as base)
function timeToEpoch(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number)
  const d = new Date(); d.setHours(h, m, 0, 0)
  return d.getTime()
}

// epoch millis → time string
function epochToTimeInput(ms: number): string {
  const d = new Date(ms)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
```

### TypeScript `verbatimModuleSyntax`
Vite's default `tsconfig.json` enables `verbatimModuleSyntax`. Type-only imports must use
`import type`:

```typescript
// CORRECT
import type { ReactNode } from 'react'
import type { UserRole } from '../store/auth'

// WRONG — compile error
import { ReactNode } from 'react'
```

### Tailwind CSS v3 setup
The generated `tailwind.config.js` has an empty `content` array. Must set:

```javascript
content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}']
```

`src/index.css` must contain only Tailwind directives (replace Vite's default styles):

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

---

## Component Conventions

### Button variants
```typescript
variant: 'primary'    // orange-500
         'secondary'  // gray-200
         'danger'     // red-500
         'ghost'      // transparent

size: 'sm' | 'md' | 'lg'
```

### StatusBadge accepted values
`'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SOLD' | 'CANCELED'`

For product active state: pass `p.status ? 'ACTIVE' : 'INACTIVE'`

### StarRating
```tsx
<StarRating value={rating} onChange={setRating} />          // interactive
<StarRating value={rating} readonly />                       // display only
```

---

## Smoke Test Checklist

```
1. Sign up as CUSTOMER → redirected to /signin → sign in → see /stores
2. Sign up as OWNER → sign in → /owner/stores → create store → add products
3. Sign in as CUSTOMER → /stores/:id → Add product → /cart → Checkout → /orders
4. Sign in as OWNER → /owner/stores/:id/orders → Mark PENDING order as Sold
5. OWNER → /owner/stores/:id/revenue → pick year/month → see ₩ total
6. CUSTOMER → /statistics/spending → pick year/month → see ₩ total
7. CUSTOMER → /orders → see order with SOLD status
```
