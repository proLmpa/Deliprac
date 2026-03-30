import client from './client'

export interface ReviewResponse {
  id: number
  storeId: number
  rating: number
  content: string
  createdAt: number
  updatedAt: number
  isOwner: boolean
}

export interface CreateReviewRequest {
  rating: number
  content: string
}

export const listReviews = (storeId: number) =>
  client.post<ReviewResponse[]>('/api/stores/reviews/list', { storeId }).then((r) => r.data)

export const createReview = (storeId: number, data: CreateReviewRequest) =>
  client.post<ReviewResponse>('/api/stores/reviews', { storeId, ...data }).then((r) => r.data)

export const deleteReview = (storeId: number, reviewId: number) =>
  client.delete('/api/stores/reviews', { data: { storeId, reviewId } })
