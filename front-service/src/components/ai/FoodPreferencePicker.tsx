import { useState } from 'react'

const CATEGORIES = ['치킨', '피자', '한식', '중식', '일식', '분식', '카페/디저트', '패스트푸드']

interface FoodPreferencePickerProps {
  onSubmit: (categories: string[]) => void
}

export default function FoodPreferencePicker({ onSubmit }: FoodPreferencePickerProps) {
  const [selected, setSelected] = useState<string[]>([])

  const toggle = (cat: string) =>
    setSelected((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    )

  return (
    <div className="bg-orange-50 border border-orange-200 rounded-xl p-4">
      <p className="text-sm font-medium text-orange-700 mb-3">
        좋아하는 음식 종류를 선택해 더 정확한 추천을 받아보세요.
      </p>
      <div className="flex flex-wrap gap-2 mb-4">
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            onClick={() => toggle(cat)}
            className={`px-3 py-1 rounded-full text-sm font-medium border transition-colors ${
              selected.includes(cat)
                ? 'bg-orange-500 text-white border-orange-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-orange-400'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>
      <button
        onClick={() => onSubmit(selected)}
        disabled={selected.length === 0}
        className="px-4 py-1.5 text-sm rounded-xl font-medium bg-orange-500 text-white hover:bg-orange-600 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        추천 받기
      </button>
    </div>
  )
}
