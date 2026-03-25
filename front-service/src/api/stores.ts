import client from './client'

export interface StoreResponse {
  id: number
  name: string
  address: string
  phone: string
  content: string
  status: 'ACTIVE' | 'INACTIVE'
  storePictureUrl: string | null
  productCreatedTime: number
  openedTime: number
  closedTime: number
  closedDays: string
  averageRating: number
  createdAt: number
  updatedAt: number
}

export interface CreateStoreRequest {
  name: string
  address: string
  phone: string
  content: string
  storePictureUrl?: string
  productCreatedTime: number
  openedTime: number
  closedTime: number
  closedDays: string
}

export interface UpdateStoreRequest {
  id: number
  name: string
  address: string
  phone: string
  content: string
  storePictureUrl?: string
  productCreatedTime: number
  openedTime: number
  closedTime: number
  closedDays: string
}

export type StoreSortBy = 'CREATED_AT' | 'RATING'

export const listStores = (sortBy: StoreSortBy = 'CREATED_AT') =>
  client.post<StoreResponse[]>('/api/stores/list', { sortBy }).then((r) => r.data)

export const getStore = (id: number) =>
  client.post<StoreResponse>('/api/stores/find', { id }).then((r) => r.data)

export const listMyStores = () =>
  client.post<StoreResponse[]>('/api/stores/mine').then((r) => r.data)

export const createStore = (data: CreateStoreRequest) =>
  client.post<StoreResponse>('/api/stores', data).then((r) => r.data)

export const updateStore = (data: UpdateStoreRequest) =>
  client.put<StoreResponse>('/api/stores', data).then((r) => r.data)

export const deactivateStore = (id: number) =>
  client.put('/api/stores/deactivate', { id })

export const getPopularProducts = (storeId: number) =>
  client.post<PopularProductResponse[]>('/api/stores/statistics/popular-products', { storeId }).then((r) => r.data)

export interface PopularProductResponse {
  id: number
  storeId: number
  name: string
  price: number
  popularity: number
  status: boolean
}
