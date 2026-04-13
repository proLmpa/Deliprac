import client from './client'

export interface NotificationItem {
  productName: string
  unitPrice: number
  quantity: number
}

export interface NotificationResponse {
  id: number
  type: 'NEW_ORDER' | 'ORDER_SOLD' | 'ORDER_CANCELED'
  title: string
  content: string
  storeId: number | null
  storeName: string | null
  read: boolean
  issuedAt: number
  expiry: number
  createdAt: number
  items: NotificationItem[]
}

export const listNotifications = (unreadOnly = false) =>
  client.post<NotificationResponse[]>('/api/notifications/list', { unreadOnly }).then((r) => r.data)

export const markRead = (notificationId: number) =>
  client.put<NotificationResponse>('/api/notifications/read', { notificationId }).then((r) => r.data)

export const markAllRead = () =>
  client.put('/api/notifications/read-all').then((r) => r.data)
