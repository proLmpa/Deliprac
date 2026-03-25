interface StarRatingProps {
  value: number
  max?: number
  onChange?: (value: number) => void
  readonly?: boolean
}

export default function StarRating({ value, max = 5, onChange, readonly = false }: StarRatingProps) {
  return (
    <div className="flex gap-0.5">
      {Array.from({ length: max }, (_, i) => i + 1).map((star) => (
        <button
          key={star}
          type="button"
          disabled={readonly}
          onClick={() => onChange?.(star)}
          className={`text-xl leading-none transition-colors ${
            star <= value ? 'text-yellow-400' : 'text-gray-300'
          } ${!readonly ? 'hover:text-yellow-400 cursor-pointer' : 'cursor-default'}`}
        >
          ★
        </button>
      ))}
    </div>
  )
}
