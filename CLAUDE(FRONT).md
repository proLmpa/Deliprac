# CLAUDE(FRONT).md

## Build & Run

```bash
cd front-service
npm install
npm run dev     # http://localhost:5173
npm run build   # production build (runs tsc -b first)
```

All traffic goes through the BFF. Start it before using the frontend:

```bash
docker compose up -d
./gradlew :bff-service:bootRun
./gradlew :notification-service:bootRun   # required for notifications and WebSocket
```

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Vite + React + TypeScript | Vite 7, React 19, TS 5.9 | App scaffolding |
| Tailwind CSS | v3 | Styling |
| React Router | v7 | SPA routing + role guards |
| Axios | latest | HTTP client with JWT interceptor |
| Zustand | v5 | Auth state (token, role, userId) |
| TanStack Query | v5 | Server-state fetching + caching |
| date-fns | v4 | Epoch millis → human-readable dates |

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
    │   ├── orders.ts          ← listStoreOrders, markSold, cancelOrder, listMyOrders,
    │   │                         getRevenue, getSpending
    │   └── notifications.ts   ← listNotifications, markAsRead
    ├── store/
    │   └── auth.ts            ← Zustand (persisted): { token, userId, role, login(), logout() }
    ├── components/
    │   ├── layout/
    │   │   ├── Header.tsx     ← nav bar with role-based links + logout button + notification badge
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
        │   └── SignUp.tsx          ← radio: CUSTOMER / OWNER
        ├── shared/
        │   └── Notifications.tsx   ← /notifications (CUSTOMER + OWNER)
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

API traffic goes to the BFF on port 8080. WebSocket connects directly to notification-service on port 8084.

```typescript
// vite.config.ts
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true },
  '/ws':  { target: 'ws://localhost:8084',  ws: true, changeOrigin: true }
}
```

---

## Routing & Role Guards

```
/signin                          → SignIn          (public)
/signup                          → SignUp          (public)
/stores                          → StoreList       (public)
/stores/:id                      → StoreDetail     (public; reviews section requires auth)
/notifications                   → Notifications   (any authenticated: CUSTOMER + OWNER)
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

JWT payload expected fields: `sub` (userId as string) and `role`.

---

## Axios Client

```typescript
// src/api/client.ts
baseURL: '/'   // relative — Vite proxy routes to BFF

// Request interceptor: Authorization: Bearer <token>
// Response interceptor: 401 or 403 → logout() + redirect /signin
```

---

## BFF API Reference

> **All read operations use `POST` with a JSON body. No path variables anywhere.**
> Resource IDs are always in the request body.

### Auth (`src/api/auth.ts`)

```typescript
signup(data)   POST /api/users/signup   { email, password, phone?, role? }  → 201 Created
signin(data)   POST /api/users/signin   { email, password }                 → { accessToken, tokenType }
```

### Stores (`src/api/stores.ts`)

```typescript
listStores(sortBy?)          POST /api/stores/list       { sortBy: 'CREATED_AT'|'RATING' }  → StoreResponse[]
getStore(id)                 POST /api/stores/find       { id }                              → StoreResponse
listMyStores()               POST /api/stores/mine       (no body)                           → StoreResponse[]
createStore(data)            POST /api/stores            { name, address, phone, content,
                                                           storePictureUrl?, productCreatedTime,
                                                           openedTime, closedTime, closedDays }
updateStore(data)            PUT  /api/stores            { id, name, address, ... }
deactivateStore(id)          PUT  /api/stores/deactivate { id }
getPopularProducts(storeId)  POST /api/stores/statistics/popular-products  { storeId }       → ProductResponse[]
```

### Products (`src/api/products.ts`)

```typescript
listProducts(storeId)                    POST /api/stores/products/list       { storeId }            → ProductResponse[]
getProduct(storeId, productId)           POST /api/stores/products/find       { storeId, productId } → ProductResponse
createProduct(data)                      POST /api/stores/products            { storeId, name, description, price, productPictureUrl? }
updateProduct(data)                      PUT  /api/stores/products            { storeId, productId, name, description, price, productPictureUrl? }
deactivateProduct(storeId, productId)    PUT  /api/stores/products/deactivate { storeId, productId }
```

### Reviews (`src/api/reviews.ts`)

```typescript
listReviews(storeId)              POST   /api/stores/reviews/list  { storeId }                   → ReviewResponse[]
createReview(data)                POST   /api/stores/reviews       { storeId, rating, content }
deleteReview(storeId, reviewId)   DELETE /api/stores/reviews       { storeId, reviewId }          ← JSON body, not path
```

### Cart (`src/api/cart.ts`)

```typescript
getCart()                              POST   /api/carts/me        (no body)                         → CartResponse
addToCart(productId, storeId, qty)     POST   /api/carts           { productId, storeId, quantity }   → CartResponse
  // ⚠ storeId is required. unitPrice is NOT sent — BFF fetches it from store-service.
removeCartItem(cartId, productId)      DELETE /api/carts/products  { cartId, productId }              ← JSON body
clearCart(cartId)                      DELETE /api/carts           { cartId }                         ← JSON body
checkout(cartId)                       PUT    /api/carts/checkout  { cartId }                         → OrderResponse
```

### Orders (`src/api/orders.ts`)

```typescript
listStoreOrders(storeId)               POST /api/stores/orders/list    { storeId }              → OrderResponse[]
markSold(storeId, orderId)             PUT  /api/stores/orders/sold    { storeId, orderId }      → OrderResponse
cancelOrder(storeId, orderId)          PUT  /api/stores/orders/cancel  { storeId, orderId }      → OrderResponse
listMyOrders()                         POST /api/users/me/orders        (no body)                → OrderResponse[]
getRevenue(storeId, year, month)       POST /api/stores/statistics/revenue   { storeId, year, month } → RevenueResponse
getSpending(year, month)               POST /api/users/me/statistics/spending { year, month }    → SpendingResponse
```

### Notifications (`src/api/notifications.ts`)

```typescript
listNotifications()     POST /api/notifications/me    (no body)              → NotificationResponse[]
markAsRead(id)          PUT  /api/notifications/read  { notificationId: id }
```

**WebSocket** (proxied via Vite `/ws` → `ws://localhost:8084`):
```typescript
const ws = new WebSocket(`/ws/notifications?token=${token}`)
ws.onmessage = (e) => {
  const notification: NotificationResponse = JSON.parse(e.data)
  // push to local notification list or show toast
}
```

---

## Backend Response Shapes

### `StoreResponse`

```typescript
{
  id: number
  name: string
  address: string
  phone: string
  content: string
  status: 'ACTIVE' | 'INACTIVE'
  storePictureUrl: string | null
  productCreatedTime: number       // epoch millis
  openedTime: number               // epoch millis
  closedTime: number               // epoch millis
  closedDays: string
  averageRating: number
  createdAt: number
  updatedAt: number
}
```

### `ProductResponse`

```typescript
{
  id: number
  storeId: number
  name: string
  description: string
  price: number
  status: boolean        // active flag — NOT named 'active'
  popularity: number
  productPictureUrl: string | null
  createdAt: number
  updatedAt: number
}
```

**Filter active products:** `products.filter(p => p.status)`

### `CartResponse`

```typescript
{
  id: number
  storeId: number
  isOrdered: boolean
  items: CartItem[]
  totalPrice: number
}
```

### `CartItem` (inside `CartResponse.items`)

```typescript
{
  id: number
  productId: number
  quantity: number
  unitPrice: number
  // NO productName — resolve via listProducts(cart.storeId)
}
```

### `OrderResponse`

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

### `RevenueResponse`

```typescript
{
  storeId: number
  year: number
  month: number
  totalRevenue: number
}
```

### `SpendingResponse`

```typescript
{
  year: number
  month: number
  totalSpending: number
}
```

### `NotificationResponse`

```typescript
{
  id: number
  title: string
  content: string
  isRead: boolean
  createdAt: number    // epoch millis
}
```

---

## Known Patterns & Pitfalls

### ⚠ API modules need rewriting
The existing `src/api/*.ts` files use REST-style GET/PUT/DELETE with path variables.
The BFF uses POST+body for all reads and IDs in the body for mutations.
Every API module must be rewritten to match the BFF API Reference above.

### DELETE with JSON body
`removeCartItem` and `clearCart` and `deleteReview` send a JSON body on DELETE requests.
Axios supports this: `client.delete(url, { data: { ... } })`.

### addToCart requires storeId
Unlike the old implementation, `addToCart` must pass `storeId` along with `productId` and `quantity`.
The BFF uses `storeId` to look up the current product price from store-service internally.

### Product name in Cart
`CartItem` does not include product names. `Cart.tsx` resolves names by fetching
`listProducts(cart.storeId)` in a second query (enabled only after cart loads):

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
function timeToEpoch(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number)
  const d = new Date(); d.setHours(h, m, 0, 0)
  return d.getTime()
}

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
`tailwind.config.js` must have:
```javascript
content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}']
```

`src/index.css` must contain only Tailwind directives:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### Notification WebSocket lifecycle
Open the WebSocket connection once on mount (e.g. in `Layout.tsx` or a custom hook) and close it on unmount. Use TanStack Query's `invalidateQueries` to refresh the notification list when a WebSocket message arrives:

```typescript
useEffect(() => {
  if (!token) return
  const ws = new WebSocket(`/ws/notifications?token=${token}`)
  ws.onmessage = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })
  return () => ws.close()
}, [token])
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
8. OWNER → /notifications → see "New order received" notification after customer checkout
9. CUSTOMER → /notifications → see "Order confirmed" notification after owner marks SOLD
10. Refresh /notifications → unread notifications persist; mark as read → isRead flag updates
```
