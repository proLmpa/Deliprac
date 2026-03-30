import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getStore } from '../../api/stores'
import { listProducts } from '../../api/products'
import { listReviews, createReview, deleteReview } from '../../api/reviews'
import { addToCart } from '../../api/cart'
import { useAuthStore } from '../../store/auth'
import { format } from 'date-fns'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StarRating from '../../components/ui/StarRating'
import StatusBadge from '../../components/ui/StatusBadge'

function epochToTime(ms: number) {
  return format(new Date(ms), 'HH:mm')
}

export default function StoreDetail() {
  const { id } = useParams<{ id: string }>()
  const storeId = Number(id)
  const { role, token } = useAuthStore()
  const queryClient = useQueryClient()

  const [reviewRating, setReviewRating] = useState(5)
  const [reviewContent, setReviewContent] = useState('')
  const [reviewError, setReviewError] = useState('')
  const [cartMsg, setCartMsg] = useState('')

  const { data: store } = useQuery({ queryKey: ['store', storeId], queryFn: () => getStore(storeId) })
  const { data: products } = useQuery({ queryKey: ['products', storeId], queryFn: () => listProducts(storeId) })
  const { data: reviews } = useQuery({
    queryKey: ['reviews', storeId],
    queryFn: () => listReviews(storeId),
    enabled: !!token,
  })

  const addMutation = useMutation({
    mutationFn: (productId: number) => addToCart(productId, storeId, 1),
    onSuccess: () => {
      setCartMsg('Added to cart!')
      setTimeout(() => setCartMsg(''), 2000)
    },
  })

  const reviewMutation = useMutation({
    mutationFn: () => createReview(storeId, { rating: reviewRating, content: reviewContent }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', storeId] })
      setReviewContent('')
      setReviewRating(5)
      setReviewError('')
    },
    onError: (err: any) => setReviewError(err.response?.data?.message ?? 'Failed to submit review.'),
  })

  const deleteMutation = useMutation({
    mutationFn: (reviewId: number) => deleteReview(storeId, reviewId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reviews', storeId] }),
  })

  if (!store) return <p className="text-center mt-10 text-gray-500">Loading…</p>

  return (
    <div className="space-y-8">
      {/* Store Header */}
      <div>
        <div className="flex items-center gap-3 mb-1">
          <h1 className="text-3xl font-bold">{store.name}</h1>
          <StatusBadge status={store.status} />
        </div>
        <p className="text-gray-600">{store.address}</p>
        <p className="text-sm text-gray-400">
          {epochToTime(store.openedTime)} – {epochToTime(store.closedTime)}
        </p>
      </div>

      {/* Products */}
      <section>
        <h2 className="text-xl font-semibold mb-3">Menu</h2>
        {cartMsg && <p className="mb-2 text-green-600 text-sm">{cartMsg}</p>}
        {products?.length === 0 && <p className="text-gray-500">No products yet.</p>}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {products?.filter((p) => p.status).map((p) => (
            <Card key={p.id} className="flex flex-col justify-between">
              <div>
                <h3 className="font-medium">{p.name}</h3>
                <p className="text-sm text-gray-500 mb-2">{p.description}</p>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-semibold text-orange-600">₩{p.price.toLocaleString()}</span>
                {role === 'CUSTOMER' && (
                  <Button
                    size="sm"
                    onClick={() => addMutation.mutate(p.id)}
                    disabled={addMutation.isPending}
                  >
                    Add
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* Reviews */}
      {token && (
        <section>
          <h2 className="text-xl font-semibold mb-3">Reviews</h2>
          {role === 'CUSTOMER' && (
            <Card className="mb-4">
              <h3 className="font-medium mb-2">Write a Review</h3>
              {reviewError && <p className="text-red-600 text-sm mb-2">{reviewError}</p>}
              <StarRating value={reviewRating} onChange={setReviewRating} />
              <textarea
                value={reviewContent}
                onChange={(e) => setReviewContent(e.target.value)}
                placeholder="Share your experience…"
                rows={3}
                className="mt-2 w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
              <Button
                size="sm"
                className="mt-2"
                onClick={() => reviewMutation.mutate()}
                disabled={!reviewContent.trim() || reviewMutation.isPending}
              >
                Submit
              </Button>
            </Card>
          )}
          {reviews?.length === 0 && <p className="text-gray-500">No reviews yet.</p>}
          <div className="space-y-3">
            {reviews?.map((r) => (
              <Card key={r.id}>
                <div className="flex justify-between items-start">
                  <div>
                    <StarRating value={r.rating} readonly />
                    <p className="text-sm mt-1">{r.content}</p>
                    <p className="text-xs text-gray-400 mt-1">
                      {format(new Date(r.createdAt), 'yyyy-MM-dd HH:mm')}
                    </p>
                  </div>
                  {(role === 'ADMIN' || r.isOwner) && (
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => deleteMutation.mutate(r.id)}
                    >
                      Delete
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
