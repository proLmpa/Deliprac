import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signin } from '../../api/auth'
import { useAuthStore } from '../../store/auth'
import { useTranslation } from '../../context/I18nContext'
import Button from '../../components/ui/Button'

export default function SignIn() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuthStore()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await signin({ email, password })
      login(res.data.accessToken)
      navigate('/stores')
    } catch {
      setError(t('auth.sign_in.error'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-sm mx-auto mt-20">
      <h1 className="text-2xl font-bold mb-6 text-center">{t('auth.sign_in.title')}</h1>
      <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg shadow">
        {error && <p className="text-red-600 text-sm">{error}</p>}
        <div>
          <label className="block text-sm font-medium mb-1">{t('auth.sign_in.email')}</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">{t('auth.sign_in.password')}</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? t('auth.sign_in.submitting') : t('auth.sign_in.submit')}
        </Button>
        <p className="text-center text-sm text-gray-500">
          {t('auth.sign_in.no_account')}{' '}
          <Link to="/signup" className="text-orange-600 hover:underline">
            {t('auth.sign_in.sign_up_link')}
          </Link>
        </p>
      </form>
    </div>
  )
}
