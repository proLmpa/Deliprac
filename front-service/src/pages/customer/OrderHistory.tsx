import { useQuery } from '@tanstack/react-query'
import { listMyOrders } from '../../api/orders'
import { format } from 'date-fns'
import Card from '../../components/ui/Card'
import StatusBadge from '../../components/ui/StatusBadge'

export default function OrderHistory() {
  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['myOrders'],
    queryFn: listMyOrders,
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading orders…</p>
  if (error) return <p className="text-center mt-10 text-red-500">Failed to load orders.</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Order History</h1>
      {orders?.length === 0 && <p className="text-gray-500">No orders yet.</p>}
      <div className="space-y-4">
        {orders?.map((order) => (
          <Card key={order.id}>
            <div className="flex justify-between items-start">
              <div>
                <p className="font-semibold">Order #{order.id}</p>
                <p className="text-sm text-gray-500">Store #{order.storeId}</p>
                <p className="text-xs text-gray-400 mt-1">
                  {format(new Date(order.createdAt), 'yyyy-MM-dd HH:mm')}
                </p>
              </div>
              <div className="text-right">
                <StatusBadge status={order.status} />
                <p className="font-semibold text-orange-600 mt-1">
                  ₩{order.totalPrice.toLocaleString()}
                </p>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
