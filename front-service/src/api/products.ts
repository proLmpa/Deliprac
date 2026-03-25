import client from './client'

export interface ProductResponse {
  id: number
  storeId: number
  name: string
  description: string
  price: number
  status: boolean
  popularity: number
  productPictureUrl: string | null
  createdAt: number
  updatedAt: number
}

export interface CreateProductRequest {
  storeId: number
  name: string
  description: string
  price: number
  productPictureUrl?: string
}

export interface UpdateProductRequest {
  storeId: number
  productId: number
  name: string
  description: string
  price: number
  productPictureUrl?: string
}

export const listProducts = (storeId: number) =>
  client.post<ProductResponse[]>('/api/stores/products/list', { storeId }).then((r) => r.data)

export const getProduct = (storeId: number, productId: number) =>
  client.post<ProductResponse>('/api/stores/products/find', { storeId, productId }).then((r) => r.data)

export const createProduct = (data: CreateProductRequest) =>
  client.post<ProductResponse>('/api/stores/products', data).then((r) => r.data)

export const updateProduct = (data: UpdateProductRequest) =>
  client.put<ProductResponse>('/api/stores/products', data).then((r) => r.data)

export const deactivateProduct = (storeId: number, productId: number) =>
  client.put('/api/stores/products/deactivate', { storeId, productId })
