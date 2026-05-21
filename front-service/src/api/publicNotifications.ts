import client from './client'

export interface PublicNotificationResponse {
  id: number
  title: string
  content: string
  isActive: boolean
  issuedAt: number
  expiresAt: number
}

export const listPublicNotifications = () =>
  client.post<PublicNotificationResponse[]>('/api/public-notifications/list').then((r) => r.data)

export const createPublicNotification = (data: { title: string; content: string; expiresAt: number }) =>
  client.post<PublicNotificationResponse>('/api/public-notifications', data).then((r) => r.data)

export const deactivatePublicNotification = (notificationId: number) =>
  client.put('/api/public-notifications/deactivate', { notificationId }).then((r) => r.data)
