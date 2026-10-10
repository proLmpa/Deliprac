import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listMyStores, deactivateStore } from '../../api/stores'
import { format } from 'date-fns'
import { useTranslation } from '../../context/I18nContext'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'

function epochToTime(ms: number) {
  return format(new Date(ms), 'HH:mm')
}

export default function MyStores() {
  const queryClient = useQueryClient()
  const { t } = useTranslation()
  const { data: stores, isLoading, error } = useQuery({
    queryKey: ['myStores'],
    queryFn: listMyStores,
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivateStore(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['myStores'] }),
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">{t('owner.stores.loading')}</p>
  if (error) return <p className="text-center mt-10 text-red-500">{t('owner.stores.error')}</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">{t('owner.stores.title')}</h1>
        <Link to="/owner/stores/new">
          <Button>{t('owner.stores.new')}</Button>
        </Link>
      </div>
      {stores?.length === 0 && <p className="text-gray-500">{t('owner.stores.empty')}</p>}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {stores?.map((store) => (
          <Card key={store.id}>
            <div className="flex justify-between items-start mb-2">
              <h2 className="font-semibold text-lg">{store.name}</h2>
              <StatusBadge status={store.status} />
            </div>
            <p className="text-sm text-gray-600 mb-1">{store.address}</p>
            <p className="text-xs text-gray-400 mb-3">
              {epochToTime(store.openedTime)} – {epochToTime(store.closedTime)}
            </p>
            <div className="flex flex-wrap gap-2">
              <Link to={`/owner/stores/${store.id}/edit`}>
                <Button variant="secondary" size="sm">{t('owner.stores.edit')}</Button>
              </Link>
              <Link to={`/owner/stores/${store.id}/products`}>
                <Button variant="secondary" size="sm">{t('owner.stores.products')}</Button>
              </Link>
              <Link to={`/owner/stores/${store.id}/orders`}>
                <Button variant="secondary" size="sm">{t('owner.stores.orders')}</Button>
              </Link>
              <Link to={`/owner/stores/${store.id}/revenue`}>
                <Button variant="secondary" size="sm">{t('owner.stores.revenue')}</Button>
              </Link>
              {store.status === 'ACTIVE' && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => deactivateMutation.mutate(store.id)}
                  disabled={deactivateMutation.isPending}
                >
                  {t('owner.stores.deactivate')}
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
