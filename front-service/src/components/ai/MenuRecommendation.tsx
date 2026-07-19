import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getRecommendations } from '../../api/ai'
import FoodPreferencePicker from './FoodPreferencePicker'
import Card from '../ui/Card'

interface MenuRecommendationProps {
  storeId: number
}

export default function MenuRecommendation({ storeId }: MenuRecommendationProps) {
  const [preferences, setPreferences] = useState<string[] | null>(null)

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['ai-recommend', storeId, preferences],
    queryFn: () => getRecommendations(storeId, preferences ?? []),
  })

  if (isLoading || isFetching) {
    return (
      <div className="space-y-3">
        <h3 className="text-base font-semibold text-orange-600">AI 추천</h3>
        <p className="text-sm text-gray-400 animate-pulse">AI가 메뉴를 분석 중이에요…</p>
      </div>
    )
  }

  if (!data) return null

  const needsPicker = data.showPreferencePicker && preferences === null

  return (
    <div className="space-y-3">
      <h3 className="text-base font-semibold text-orange-600">AI 추천</h3>

      {needsPicker ? (
        <FoodPreferencePicker onSubmit={setPreferences} />
      ) : data.recommendations.length === 0 ? (
        <p className="text-sm text-gray-500">추천 결과를 불러올 수 없어요.</p>
      ) : (
        data.recommendations.map((item) => (
          <Card key={item.productName} className="flex gap-3 items-start py-3">
            <span className="text-orange-400 text-base leading-none mt-0.5">★</span>
            <div>
              <p className="font-medium text-sm">{item.productName}</p>
              <p className="text-xs text-gray-500 mt-0.5">{item.reason}</p>
            </div>
          </Card>
        ))
      )}
    </div>
  )
}
