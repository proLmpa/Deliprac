import apiClient from './client'

export function fetchMessages(lang: string): Promise<Record<string, string>> {
  return apiClient.post<Record<string, string>>('/api/i18n/messages', { lang })
    .then(r => r.data)
}
