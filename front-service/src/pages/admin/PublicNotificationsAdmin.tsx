import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import {
  listPublicNotifications,
  createPublicNotification,
  deactivatePublicNotification,
} from '../../api/publicNotifications'
import Button from '../../components/ui/Button'

export default function PublicNotificationsAdmin() {
  const queryClient = useQueryClient()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [error, setError] = useState('')

  const { data: notifications = [] } = useQuery({
    queryKey: ['public-notifications'],
    queryFn: listPublicNotifications,
    staleTime: 60_000,
  })

  const createMutation = useMutation({
    mutationFn: () =>
      createPublicNotification({
        title,
        content,
        expiresAt: new Date(expiresAt).getTime(),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['public-notifications'] })
      setTitle('')
      setContent('')
      setExpiresAt('')
      setError('')
    },
    onError: (err: any) =>
      setError(err.response?.data?.detail ?? 'Failed to create announcement.'),
  })

  const deactivateMutation = useMutation({
    mutationFn: deactivatePublicNotification,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['public-notifications'] }),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!title.trim() || !content.trim() || !expiresAt) {
      setError('All fields are required.')
      return
    }
    if (new Date(expiresAt).getTime() <= Date.now()) {
      setError('Expiry must be in the future.')
      return
    }
    createMutation.mutate()
  }

  const active = notifications.filter((n) => n.isActive)

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold">Manage Announcements</h1>

      <section className="bg-white p-6 rounded-lg shadow space-y-4">
        <h2 className="text-lg font-semibold">New Announcement</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <div>
            <label className="block text-sm font-medium mb-1">Title</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Content</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={3}
              required
              className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Expires At</label>
            <input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              required
              className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
          <div className="flex justify-end">
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Creating…' : 'Create'}
            </Button>
          </div>
        </form>
      </section>

      <section className="bg-white p-6 rounded-lg shadow space-y-3">
        <h2 className="text-lg font-semibold">Active Announcements</h2>
        {active.length === 0 ? (
          <p className="text-gray-500 text-sm">No active announcements.</p>
        ) : (
          <ul className="space-y-3">
            {active.map((n) => (
              <li key={n.id} className="flex items-start justify-between gap-4 border rounded p-3">
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-sm">{n.title}</p>
                  <p className="text-sm text-gray-600 mt-0.5">{n.content}</p>
                  <p className="text-xs text-gray-400 mt-1">
                    Expires {format(n.expiresAt, 'yyyy-MM-dd HH:mm')}
                  </p>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => deactivateMutation.mutate(n.id)}
                  disabled={deactivateMutation.isPending}
                >
                  Deactivate
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
