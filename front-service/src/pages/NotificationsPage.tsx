import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { useNavigate } from 'react-router-dom'
import { listNotifications, markRead, markAllRead, type NotificationResponse } from '../api/notifications'
import Button from '../components/ui/Button'

export default function NotificationsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => listNotifications(false),
    refetchInterval: 30_000,
  })

  const markReadMutation = useMutation({
    mutationFn: markRead,
    onSuccess: (_, notificationId) => {
      queryClient.setQueryData(['notifications'], (old: NotificationResponse[] | undefined) =>
        old?.map((n) => (n.id === notificationId ? { ...n, read: true } : n)) ?? []
      )
    },
  })

  const markAllReadMutation = useMutation({
    mutationFn: markAllRead,
    onSuccess: () => {
      queryClient.setQueryData(['notifications'], (old: NotificationResponse[] | undefined) =>
        old?.map((n) => ({ ...n, read: true })) ?? []
      )
    },
  })

  function handleNotificationClick(n: NotificationResponse) {
    if (!n.read) markReadMutation.mutate(n.id)
    if (n.type === 'NEW_ORDER' && n.storeId !== null) {
      navigate(`/owner/stores/${n.storeId}/orders`)
    } else if (n.type === 'ORDER_SOLD' || n.type === 'ORDER_CANCELED') {
      navigate('/orders')
    }
  }

  if (isLoading) return <div className="p-6 text-gray-500">Loading...</div>

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">Notifications</h1>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => markAllReadMutation.mutate()}
          disabled={notifications.every((n) => n.read)}
        >
          Mark all read
        </Button>
      </div>

      {notifications.length === 0 ? (
        <p className="text-gray-500 text-center py-8">No notifications yet.</p>
      ) : (
        <ul className="space-y-2">
          {notifications.map((n) => (
            <li
              key={n.id}
              onClick={() => handleNotificationClick(n)}
              className={`p-4 rounded-lg border flex items-start justify-between gap-4 cursor-pointer hover:brightness-95 ${
                n.read ? 'bg-gray-100 border-gray-200' : 'bg-orange-50 border-orange-200'
              }`}
            >
              <div className="flex-1 min-w-0">
                <p className={`font-semibold ${n.read ? 'text-gray-400' : 'text-gray-900'}`}>
                  {n.title}
                </p>
                <div className={`text-sm mt-0.5 ${n.read ? 'text-gray-400' : 'text-gray-600'}`}>
                  {n.items.length > 0 ? (
                    <table className="text-sm w-full mt-1">
                      <tbody>
                        {n.items.map((item, i) => (
                          <tr key={i}>
                            <td className="pr-3">{item.productName}</td>
                            <td className={`pr-3 ${n.read ? 'text-gray-400' : 'text-gray-500'}`}>₩{item.unitPrice.toLocaleString()}</td>
                            <td>× {item.quantity}</td>
                          </tr>
                        ))}
                        <tr className="border-t border-gray-200">
                          <td colSpan={2} className="pt-1 font-semibold">Total</td>
                          <td className="pt-1 font-semibold">
                            ₩{n.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0).toLocaleString()}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  ) : (
                    <p>{n.content}</p>
                  )}
                  {n.storeName && (
                    <p className="text-xs text-gray-500 mt-0.5">{n.storeName}</p>
                  )}
                </div>
                <p className={`text-xs mt-1 ${n.read ? 'text-gray-300' : 'text-gray-400'}`}>{format(n.issuedAt, 'yyyy-MM-dd HH:mm')}</p>
              </div>
              {!n.read && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={(e) => { e.stopPropagation(); markReadMutation.mutate(n.id) }}
                >
                  Mark read
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
