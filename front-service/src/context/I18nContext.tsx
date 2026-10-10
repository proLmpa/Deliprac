import { createContext, useContext, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useLocaleStore } from '../store/locale'
import { fetchMessages } from '../api/i18n'

type Params = Record<string, string | number>

interface I18nContextType {
  t: (key: string, params?: Params) => string
}

const I18nContext = createContext<I18nContextType>({ t: (k) => k })

export function I18nProvider({ children }: { children: ReactNode }) {
  const { locale } = useLocaleStore()

  const { data: messages = {} } = useQuery({
    queryKey: ['i18n', locale],
    queryFn: () => fetchMessages(locale),
    staleTime: Infinity,
  })

  const t = (key: string, params?: Params): string => {
    let msg = messages[key] ?? key
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        msg = msg.replaceAll(`{${k}}`, String(v))
      })
    }
    return msg
  }

  return <I18nContext.Provider value={{ t }}>{children}</I18nContext.Provider>
}

export const useTranslation = () => useContext(I18nContext)