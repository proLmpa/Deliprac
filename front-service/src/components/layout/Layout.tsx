import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import PublicNotificationBanner from './PublicNotificationBanner'
import ChatWindow from '../ai/ChatWindow'
import { useAuthStore } from '../../store/auth'

export default function Layout() {
  const { role, token } = useAuthStore()
  const [aiConsent, setAiConsent] = useState(() => localStorage.getItem('aiConsent'))

  useEffect(() => {
    const handler = () => setAiConsent(localStorage.getItem('aiConsent'))
    window.addEventListener('ai-consent-changed', handler)
    return () => window.removeEventListener('ai-consent-changed', handler)
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <PublicNotificationBanner />
      <main className="max-w-6xl mx-auto px-4 py-6">
        <Outlet />
      </main>
      {role === 'CUSTOMER' && !!token && aiConsent === 'yes' && <ChatWindow />}
    </div>
  )
}
