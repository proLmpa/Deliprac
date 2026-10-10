import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../../api/auth'
import { useTranslation } from '../../context/I18nContext'
import Button from '../../components/ui/Button'

export default function SignUp() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'CUSTOMER' | 'OWNER'>('CUSTOMER')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { t } = useTranslation()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signup({ email, password, role })
      navigate('/signin')
    } catch (err: any) {
      setError(err.response?.data?.message ?? t('auth.sign_up.error'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-sm mx-auto mt-20">
      <h1 className="text-2xl font-bold mb-6 text-center">{t('auth.sign_up.title')}</h1>
      <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg shadow">
        {error && <p className="text-red-600 text-sm">{error}</p>}
        <div>
          <label className="block text-sm font-medium mb-1">{t('auth.sign_up.email')}</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">{t('auth.sign_up.password')}</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">{t('auth.sign_up.role')}</label>
          <div className="flex gap-4">
            {(['CUSTOMER', 'OWNER'] as const).map((r) => (
              <label key={r} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  value={r}
                  checked={role === r}
                  onChange={() => setRole(r)}
                  className="accent-orange-500"
                />
                <span className="text-sm">{r}</span>
              </label>
            ))}
          </div>
        </div>
        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? t('auth.sign_up.submitting') : t('auth.sign_up.submit')}
        </Button>
        <p className="text-center text-sm text-gray-500">
          {t('auth.sign_up.already_account')}{' '}
          <Link to="/signin" className="text-orange-600 hover:underline">
            {t('auth.sign_up.sign_in_link')}
          </Link>
        </p>
      </form>
    </div>
  )
}
