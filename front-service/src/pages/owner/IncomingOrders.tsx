import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listStoreOrders, markSold, cancelOrder } from '../../api/orders'
import { format } from 'date-fns'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'

export default function IncomingOrders() {
  const { id } = useParams<{ id: string }>()
  const storeId = Number(id)
  const queryClient = useQueryClient()

  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['storeOrders', storeId],
    queryFn: () => listStoreOrders(storeId),
  })

  const soldMutation = useMutation({
    mutationFn: (orderId: number) => markSold(storeId, orderId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['storeOrders', storeId] }),
  })

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => cancelOrder(storeId, orderId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['storeOrders', storeId] }),
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading orders…</p>
  if (error) return <p className="text-center mt-10 text-red-500">Failed to load orders.</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Incoming Orders (Store #{storeId})</h1>
      {orders?.length === 0 && <p className="text-gray-500">No orders yet.</p>}
      <div className="space-y-4">
        {orders?.map((order) => (
          <Card key={order.id}>
            <div className="flex justify-between items-center">
              <div>
                <p className="font-semibold">Order #{order.id}</p>
                <p className="text-xs text-gray-400">
                  {format(new Date(order.createdAt), 'yyyy-MM-dd HH:mm')}
                </p>
                <p className="font-semibold text-orange-600 mt-1">
                  ₩{order.totalPrice.toLocaleString()}
                </p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <StatusBadge status={order.status} />
                {order.status === 'PENDING' && (
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => soldMutation.mutate(order.id)}
                      disabled={soldMutation.isPending}
                    >
                      Mark Sold
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => cancelMutation.mutate(order.id)}
                      disabled={cancelMutation.isPending}
                    >
                      Cancel
                    </Button>
                  </div>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
