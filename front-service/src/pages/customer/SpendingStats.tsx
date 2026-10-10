import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getSpending } from '../../api/orders'
import { useTranslation } from '../../context/I18nContext'
import Card from '../../components/ui/Card'

export default function SpendingStats() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const { t } = useTranslation()

  const { data, isLoading, error } = useQuery({
    queryKey: ['spending', year, month],
    queryFn: () => getSpending(year, month),
  })

  return (
    <div className="max-w-md mx-auto">
      <h1 className="text-2xl font-bold mb-6">{t('stats.spending.title')}</h1>
      <Card className="mb-6">
        <div className="flex gap-4 items-end">
          <div>
            <label className="block text-sm font-medium mb-1">{t('stats.spending.year')}</label>
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
            <label className="block text-sm font-medium mb-1">{t('stats.spending.month')}</label>
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
      {isLoading && <p className="text-gray-500">{t('stats.spending.loading')}</p>}
      {error && <p className="text-red-500">{t('stats.spending.error')}</p>}
      {data && (
        <Card>
          <p className="text-lg text-gray-600">
            {data.year}/{String(data.month).padStart(2, '0')} spending
          </p>
          <p className="text-4xl font-bold text-orange-600 mt-2">
            ₩{data.totalSpending.toLocaleString()}
          </p>
        </Card>
      )}
    </div>
  )
}
