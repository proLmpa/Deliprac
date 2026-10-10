import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listStores } from '../../api/stores'
import { format } from 'date-fns'
import { useTranslation } from '../../context/I18nContext'
import Card from '../../components/ui/Card'
import StatusBadge from '../../components/ui/StatusBadge'

function epochToTime(ms: number) {
  return format(new Date(ms), 'HH:mm')
}

export default function StoreList() {
  const { t } = useTranslation()
  const { data: stores, isLoading, error } = useQuery({
    queryKey: ['stores'],
    queryFn: () => listStores(),
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">{t('store.list.loading')}</p>
  if (error) return <p className="text-center mt-10 text-red-500">{t('store.list.error')}</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">{t('store.list.title')}</h1>
      {stores?.length === 0 && (
        <p className="text-gray-500">{t('store.list.empty')}</p>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {stores?.map((store) => (
          <Link key={store.id} to={`/stores/${store.id}`}>
            <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
              <div className="flex justify-between items-start mb-2">
                <h2 className="font-semibold text-lg">{store.name}</h2>
                <StatusBadge status={store.status} />
              </div>
              <p className="text-sm text-gray-600 mb-2">{store.address}</p>
              <p className="text-xs text-gray-400">
                {epochToTime(store.openedTime)} – {epochToTime(store.closedTime)}
              </p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
