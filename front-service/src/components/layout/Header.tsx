import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '../../store/auth'
import { useLocaleStore } from '../../store/locale'
import { listNotifications } from '../../api/notifications'
import { useTranslation } from '../../context/I18nContext'
import Button from '../ui/Button'

export default function Header() {
  const { role, token, logout } = useAuthStore()
  const { locale, setLocale } = useLocaleStore()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const { data: unread = [] } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => listNotifications(true),
    enabled: !!token,
    refetchInterval: 30_000,
  })

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
            {t('nav.stores')}
          </Link>
          {token && role === 'CUSTOMER' && (
            <>
              <Link to="/cart" className="hover:underline">
                {t('nav.cart')}
              </Link>
              <Link to="/orders" className="hover:underline">
                {t('nav.orders')}
              </Link>
              <Link to="/statistics/spending" className="hover:underline">
                {t('nav.spending')}
              </Link>
            </>
          )}
          {token && role === 'OWNER' && (
            <>
              <Link to="/owner/stores" className="hover:underline">
                {t('nav.my_stores')}
              </Link>
            </>
          )}
          {token && role === 'ADMIN' && (
            <Link to="/admin/public-notifications" className="hover:underline">
              {t('nav.announcements')}
            </Link>
          )}
          {token && (
            <Link to="/notifications" className="relative hover:underline">
              🔔
              {unread.length > 0 && (
                <span className="absolute -top-1.5 -right-2 bg-red-600 text-white text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {unread.length > 9 ? '9+' : unread.length}
                </span>
              )}
            </Link>
          )}
          {token ? (
            <Button variant="ghost" size="sm" onClick={handleLogout} className="text-white hover:bg-orange-600">
              {t('nav.logout')}
            </Button>
          ) : (
            <>
              <Link to="/signin" className="hover:underline">
                {t('nav.sign_in')}
              </Link>
              <Link to="/signup" className="hover:underline">
                {t('nav.sign_up')}
              </Link>
            </>
          )}
          <button
            onClick={() => setLocale(locale === 'en' ? 'ko' : 'en')}
            className="px-2 py-1 bg-orange-600 hover:bg-orange-700 rounded text-sm font-medium"
          >
            {locale === 'en' ? '한국어' : 'English'}
          </button>
        </nav>
      </div>
    </header>
  )
}
