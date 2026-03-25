import client from './client'

export interface OrderResponse {
  id: number
  storeId: number
  totalPrice: number
  status: 'PENDING' | 'SOLD' | 'CANCELED'
  createdAt: number
  updatedAt: number
}

export interface RevenueResponse {
  storeId: number
  year: number
  month: number
  totalRevenue: number
}

export interface SpendingResponse {
  year: number
  month: number
  totalSpending: number
}

export const listStoreOrders = (storeId: number) =>
  client.post<OrderResponse[]>('/api/stores/orders/list', { storeId }).then((r) => r.data)

export const markSold = (storeId: number, orderId: number) =>
  client.put<OrderResponse>('/api/stores/orders/sold', { storeId, orderId }).then((r) => r.data)

export const cancelOrder = (storeId: number, orderId: number) =>
  client.put<OrderResponse>('/api/stores/orders/cancel', { storeId, orderId }).then((r) => r.data)

export const listMyOrders = () =>
  client.post<OrderResponse[]>('/api/users/me/orders').then((r) => r.data)

export const getRevenue = (storeId: number, year: number, month: number) =>
  client.post<RevenueResponse>('/api/stores/statistics/revenue', { storeId, year, month }).then((r) => r.data)

export const getSpending = (year: number, month: number) =>
  client.post<SpendingResponse>('/api/users/me/statistics/spending', { year, month }).then((r) => r.data)
