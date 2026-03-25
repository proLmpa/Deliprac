import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listStores } from '../../api/stores'
import { format } from 'date-fns'
import Card from '../../components/ui/Card'
import StatusBadge from '../../components/ui/StatusBadge'

function epochToTime(ms: number) {
  return format(new Date(ms), 'HH:mm')
}

export default function StoreList() {
  const { data: stores, isLoading, error } = useQuery({
    queryKey: ['stores'],
    queryFn: () => listStores(),
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading stores…</p>
  if (error) return <p className="text-center mt-10 text-red-500">Failed to load stores.</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">All Stores</h1>
      {stores?.length === 0 && (
        <p className="text-gray-500">No stores available yet.</p>
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
