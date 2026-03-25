import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getStore, createStore, updateStore } from '../../api/stores'
import Button from '../../components/ui/Button'

function timeToEpoch(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m, 0, 0)
  return d.getTime()
}

function epochToTimeInput(ms: number): string {
  const d = new Date(ms)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export default function StoreForm() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const storeId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [content, setContent] = useState('')
  const [openedTime, setOpenedTime] = useState('09:00')
  const [closedTime, setClosedTime] = useState('22:00')
  const [closedDays, setClosedDays] = useState('')
  const [error, setError] = useState('')

  const { data: store } = useQuery({
    queryKey: ['store', storeId],
    queryFn: () => getStore(storeId),
    enabled: isEdit,
  })

  useEffect(() => {
    if (store) {
      setName(store.name)
      setAddress(store.address)
      setPhone(store.phone)
      setContent(store.content)
      setOpenedTime(epochToTimeInput(store.openedTime))
      setClosedTime(epochToTimeInput(store.closedTime))
      setClosedDays(store.closedDays)
    }
  }, [store])

  const commonFields = () => ({
    name,
    address,
    phone,
    content,
    openedTime: timeToEpoch(openedTime),
    closedTime: timeToEpoch(closedTime),
    closedDays,
    productCreatedTime: Date.now(),
  })

  const createMutation = useMutation({
    mutationFn: () => createStore(commonFields()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myStores'] })
      navigate('/owner/stores')
    },
    onError: (err: any) => setError(err.response?.data?.message ?? 'Failed to create store.'),
  })

  const updateMutation = useMutation({
    mutationFn: () => updateStore({ id: storeId, ...commonFields() }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myStores'] })
      queryClient.invalidateQueries({ queryKey: ['store', storeId] })
      navigate('/owner/stores')
    },
    onError: (err: any) => setError(err.response?.data?.message ?? 'Failed to update store.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!name.trim() || !address.trim() || !phone.trim() || !content.trim()) {
      setError('Name, address, phone, and description are required.')
      return
    }
    isEdit ? updateMutation.mutate() : createMutation.mutate()
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-6">{isEdit ? 'Edit Store' : 'New Store'}</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow space-y-4">
        {error && <p className="text-red-600 text-sm">{error}</p>}
        <div>
          <label className="block text-sm font-medium mb-1">Store Name</label>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Address</label>
          <input
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Phone</label>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="02-1234-5678"
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Description</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={3}
            placeholder="Describe your store…"
            required
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div className="flex gap-4">
          <div className="flex-1">
            <label className="block text-sm font-medium mb-1">Open Time</label>
            <input
              type="time"
              value={openedTime}
              onChange={(e) => setOpenedTime(e.target.value)}
              className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
          <div className="flex-1">
            <label className="block text-sm font-medium mb-1">Close Time</label>
            <input
              type="time"
              value={closedTime}
              onChange={(e) => setClosedTime(e.target.value)}
              className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
            />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Closed Days</label>
          <input
            value={closedDays}
            onChange={(e) => setClosedDays(e.target.value)}
            placeholder="e.g. Mon,Tue or leave empty"
            className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate('/owner/stores')}>
            Cancel
          </Button>
          <Button type="submit" disabled={isPending}>
            {isPending ? 'Saving…' : isEdit ? 'Update' : 'Create'}
          </Button>
        </div>
      </form>
    </div>
  )
}
