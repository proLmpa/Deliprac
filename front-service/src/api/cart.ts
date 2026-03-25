import client from './client'

export interface CartItem {
  id: number
  productId: number
  unitPrice: number
  quantity: number
}

export interface CartResponse {
  id: number
  storeId: number
  isOrdered: boolean
  items: CartItem[]
  totalPrice: number
}

export const getCart = () =>
  client.post<CartResponse>('/api/carts/me').then((r) => r.data)

export const addToCart = (productId: number, storeId: number, quantity: number) =>
  client.post<CartResponse>('/api/carts', { productId, storeId, quantity }).then((r) => r.data)

export const removeCartItem = (cartId: number, productId: number) =>
  client.delete<CartResponse>('/api/carts/products', { data: { cartId, productId } }).then((r) => r.data)

export const clearCart = (cartId: number) =>
  client.delete('/api/carts', { data: { cartId } })

export const checkout = (cartId: number) =>
  client.put<{ id: number; status: string; totalPrice: number }>('/api/carts/checkout', { cartId }).then((r) => r.data)
