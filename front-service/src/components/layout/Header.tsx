import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/auth'
import Button from '../ui/Button'

export default function Header() {
  const { role, token, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/signin')
  }

  return (
    <header className="bg-orange-500 text-white shadow-md">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/stores" className="text-xl font-bold tracking-tight">
          🍱 Baemin
        </Link>
        <nav className="flex items-center gap-4 text-sm">
          <Link to="/stores" className="hover:underline">
            Stores
          </Link>
          {token && role === 'CUSTOMER' && (
            <>
              <Link to="/cart" className="hover:underline">
                Cart
              </Link>
              <Link to="/orders" className="hover:underline">
                Orders
              </Link>
              <Link to="/statistics/spending" className="hover:underline">
                Spending
              </Link>
            </>
          )}
          {token && role === 'OWNER' && (
            <>
              <Link to="/owner/stores" className="hover:underline">
                My Stores
              </Link>
            </>
          )}
          {token ? (
            <Button variant="ghost" size="sm" onClick={handleLogout} className="text-white hover:bg-orange-600">
              Logout
            </Button>
          ) : (
            <>
              <Link to="/signin" className="hover:underline">
                Sign In
              </Link>
              <Link to="/signup" className="hover:underline">
                Sign Up
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
