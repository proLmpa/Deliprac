import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from './store/auth'
import type { UserRole } from './store/auth'
import Layout from './components/layout/Layout'

// Auth pages
import SignIn from './pages/auth/SignIn'
import SignUp from './pages/auth/SignUp'

// Customer pages
import StoreList from './pages/customer/StoreList'
import StoreDetail from './pages/customer/StoreDetail'
import Cart from './pages/customer/Cart'
import OrderHistory from './pages/customer/OrderHistory'
import SpendingStats from './pages/customer/SpendingStats'

// Shared pages
import NotificationsPage from './pages/NotificationsPage'

// Owner pages
import MyStores from './pages/owner/MyStores'
import StoreForm from './pages/owner/StoreForm'
import ProductList from './pages/owner/ProductList'
import IncomingOrders from './pages/owner/IncomingOrders'
import RevenueStats from './pages/owner/RevenueStats'

function RequireAuth({ children, role }: { children: ReactNode; role?: UserRole }) {
  const { token, role: userRole } = useAuthStore()
  if (!token) return <Navigate to="/signin" replace />
  if (role && userRole !== role) return <Navigate to="/stores" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          {/* Public */}
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />
          <Route path="/stores" element={<StoreList />} />
          <Route path="/stores/:id" element={<StoreDetail />} />

          {/* All authenticated roles */}
          <Route
            path="/notifications"
            element={<RequireAuth><NotificationsPage /></RequireAuth>}
          />

          {/* Customer only */}
          <Route
            path="/cart"
            element={<RequireAuth role="CUSTOMER"><Cart /></RequireAuth>}
          />
          <Route
            path="/orders"
            element={<RequireAuth role="CUSTOMER"><OrderHistory /></RequireAuth>}
          />
          <Route
            path="/statistics/spending"
            element={<RequireAuth role="CUSTOMER"><SpendingStats /></RequireAuth>}
          />

          {/* Owner only */}
          <Route
            path="/owner/stores"
            element={<RequireAuth role="OWNER"><MyStores /></RequireAuth>}
          />
          <Route
            path="/owner/stores/new"
            element={<RequireAuth role="OWNER"><StoreForm /></RequireAuth>}
          />
          <Route
            path="/owner/stores/:id/edit"
            element={<RequireAuth role="OWNER"><StoreForm /></RequireAuth>}
          />
          <Route
            path="/owner/stores/:id/products"
            element={<RequireAuth role="OWNER"><ProductList /></RequireAuth>}
          />
          <Route
            path="/owner/stores/:id/orders"
            element={<RequireAuth role="OWNER"><IncomingOrders /></RequireAuth>}
          />
          <Route
            path="/owner/stores/:id/revenue"
            element={<RequireAuth role="OWNER"><RevenueStats /></RequireAuth>}
          />

          {/* Default */}
          <Route path="/" element={<Navigate to="/stores" replace />} />
          <Route path="*" element={<Navigate to="/stores" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
