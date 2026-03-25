import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type UserRole = 'CUSTOMER' | 'OWNER' | 'ADMIN'

interface AuthState {
  token: string | null
  userId: number | null
  role: UserRole | null
  login: (token: string) => void
  logout: () => void
}

function decodeJwtPayload(token: string): { id: number; role: UserRole } | null {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return { id: decoded.id ?? decoded.sub, role: decoded.role }
  } catch {
    return null
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      role: null,
      login: (token: string) => {
        const payload = decodeJwtPayload(token)
        set({ token, userId: payload?.id ?? null, role: payload?.role ?? null })
      },
      logout: () => set({ token: null, userId: null, role: null }),
    }),
    { name: 'baemin-auth' }
  )
)
