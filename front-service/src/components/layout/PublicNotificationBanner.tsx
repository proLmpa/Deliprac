import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listPublicNotifications } from '../../api/publicNotifications'

export default function PublicNotificationBanner() {
  const [index, setIndex] = useState(0)
  const [dismissed, setDismissed] = useState<Set<number>>(new Set())

  const { data = [] } = useQuery({
    queryKey: ['public-notifications'],
    queryFn: listPublicNotifications,
    staleTime: 60_000,
    retry: false,
  })

  const visible = data.filter((n) => n.expiresAt > Date.now() && !dismissed.has(n.id))

  if (visible.length === 0) return null

  const safeIndex = Math.min(index, visible.length - 1)
  const current = visible[safeIndex]

  const prev = () => setIndex((i) => Math.max(0, i - 1))
  const next = () => setIndex((i) => Math.min(visible.length - 1, i + 1))

  const dismiss = () => {
    setDismissed((prev) => new Set(prev).add(current.id))
    setIndex((i) => (i >= visible.length - 1 ? Math.max(0, i - 1) : i))
  }

  return (
    <div className="bg-amber-100 border-b border-amber-300 text-amber-900">
      <div className="max-w-6xl mx-auto px-4 py-2 flex items-center gap-3">
        {visible.length > 1 && (
          <button
            onClick={prev}
            disabled={safeIndex === 0}
            className="text-amber-700 hover:text-amber-900 disabled:opacity-30 font-bold text-lg leading-none"
            aria-label="Previous announcement"
          >
            ‹
          </button>
        )}
        <div className="flex-1 min-w-0 flex items-baseline gap-2">
          <span className="font-bold text-sm shrink-0">{current.title}</span>
          <span className="text-sm truncate">{current.content}</span>
        </div>
        {visible.length > 1 && (
          <>
            <span className="text-xs text-amber-700 shrink-0">
              {safeIndex + 1} / {visible.length}
            </span>
            <button
              onClick={next}
              disabled={safeIndex === visible.length - 1}
              className="text-amber-700 hover:text-amber-900 disabled:opacity-30 font-bold text-lg leading-none"
              aria-label="Next announcement"
            >
              ›
            </button>
          </>
        )}
        <button
          onClick={dismiss}
          className="text-amber-700 hover:text-amber-900 font-bold text-lg leading-none ml-1"
          aria-label="Dismiss announcement"
        >
          ×
        </button>
      </div>
    </div>
  )
}
