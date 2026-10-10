import { useQuery } from '@tanstack/react-query'
import { listMyOrders } from '../../api/orders'
import { format } from 'date-fns'
import { useTranslation } from '../../context/I18nContext'
import Card from '../../components/ui/Card'
import StatusBadge from '../../components/ui/StatusBadge'

export default function OrderHistory() {
  const { t } = useTranslation()
  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['myOrders'],
    queryFn: listMyOrders,
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">{t('order.history.loading')}</p>
  if (error) return <p className="text-center mt-10 text-red-500">{t('order.history.error')}</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">{t('order.history.title')}</h1>
      {orders?.length === 0 && <p className="text-gray-500">{t('order.history.empty')}</p>}
      <div className="space-y-4">
        {orders?.map((order) => (
          <Card key={order.id}>
            <div className="flex justify-between items-start">
              <div>
                <p className="font-semibold">{t('order.history.order_id')}{order.id}</p>
                <p className="text-sm text-gray-500">{t('order.history.store_id')}{order.storeId}</p>
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
