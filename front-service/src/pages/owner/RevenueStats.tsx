import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getRevenue } from '../../api/orders'
import Card from '../../components/ui/Card'

export default function RevenueStats() {
  const { id } = useParams<{ id: string }>()
  const storeId = Number(id)
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const { data, isLoading, error } = useQuery({
    queryKey: ['revenue', storeId, year, month],
    queryFn: () => getRevenue(storeId, year, month),
  })

  return (
    <div className="max-w-md mx-auto">
      <h1 className="text-2xl font-bold mb-6">Revenue (Store #{storeId})</h1>
      <Card className="mb-6">
        <div className="flex gap-4 items-end">
          <div>
            <label className="block text-sm font-medium mb-1">Year</label>
            <input
              type="number"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
              min={2020}
              max={2099}
              className="border rounded px-3 py-2 w-24 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Month</label>
            <select
              value={month}
              onChange={(e) => setMonth(Number(e.target.value))}
              className="border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Card>
      {isLoading && <p className="text-gray-500">Loading…</p>}
      {error && <p className="text-red-500">Failed to load revenue.</p>}
      {data && (
        <Card>
          <p className="text-lg text-gray-600">
            {data.year}/{String(data.month).padStart(2, '0')} revenue
          </p>
          <p className="text-4xl font-bold text-orange-600 mt-2">
            ₩{data.totalRevenue.toLocaleString()}
          </p>
        </Card>
      )}
    </div>
  )
}
