import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listProducts, createProduct, updateProduct, deactivateProduct } from '../../api/products'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'

interface ProductForm {
  name: string
  description: string
  price: string
}

const emptyForm: ProductForm = { name: '', description: '', price: '' }

export default function ProductList() {
  const { id } = useParams<{ id: string }>()
  const storeId = Number(id)
  const queryClient = useQueryClient()

  const [showForm, setShowForm] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<ProductForm>(emptyForm)
  const [formError, setFormError] = useState('')

  const { data: products, isLoading } = useQuery({
    queryKey: ['products', storeId],
    queryFn: () => listProducts(storeId),
  })

  const createMutation = useMutation({
    mutationFn: () =>
      createProduct({ storeId, name: form.name, description: form.description, price: Number(form.price) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', storeId] })
      setShowForm(false)
      setForm(emptyForm)
      setFormError('')
    },
    onError: (err: any) => setFormError(err.response?.data?.message ?? 'Failed.'),
  })

  const updateMutation = useMutation({
    mutationFn: () =>
      updateProduct({ storeId, productId: editId!, name: form.name, description: form.description, price: Number(form.price) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', storeId] })
      setEditId(null)
      setForm(emptyForm)
      setFormError('')
    },
    onError: (err: any) => setFormError(err.response?.data?.message ?? 'Failed.'),
  })

  const deactivateMutation = useMutation({
    mutationFn: (productId: number) => deactivateProduct(storeId, productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products', storeId] }),
  })

  const startEdit = (p: { id: number; name: string; description: string; price: number }) => {
    setEditId(p.id)
    setForm({ name: p.name, description: p.description, price: String(p.price) })
    setShowForm(false)
    setFormError('')
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.name.trim() || !form.price) { setFormError('Name and price required.'); return }
    setFormError('')
    editId !== null ? updateMutation.mutate() : createMutation.mutate()
  }

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading…</p>

  const activeForm = showForm || editId !== null

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Products (Store #{storeId})</h1>
        {!activeForm && (
          <Button onClick={() => { setShowForm(true); setEditId(null); setForm(emptyForm) }}>
            + Add Product
          </Button>
        )}
      </div>

      {activeForm && (
        <form onSubmit={handleSubmit} className="bg-white p-4 rounded-lg shadow mb-6 space-y-3">
          <h2 className="font-semibold">{editId !== null ? 'Edit Product' : 'New Product'}</h2>
          {formError && <p className="text-red-600 text-sm">{formError}</p>}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Price (₩)</label>
              <input
                type="number"
                min={0}
                value={form.price}
                onChange={(e) => setForm({ ...form, price: e.target.value })}
                required
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
            </div>
          </div>
          <div className="flex gap-2 justify-end">
            <Button
              type="button"
              variant="secondary"
              onClick={() => { setShowForm(false); setEditId(null); setForm(emptyForm) }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {editId !== null ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      )}

      {products?.length === 0 && <p className="text-gray-500">No products yet.</p>}
      <div className="space-y-3">
        {products?.map((p) => (
          <Card key={p.id} className="flex justify-between items-center">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-medium">{p.name}</span>
                <StatusBadge status={p.status ? 'ACTIVE' : 'INACTIVE'} />
              </div>
              <p className="text-sm text-gray-500">{p.description}</p>
              <p className="text-sm font-semibold text-orange-600">₩{p.price.toLocaleString()}</p>
              <p className="text-xs text-gray-400">Popularity: {p.popularity}</p>
            </div>
            <div className="flex gap-2 flex-shrink-0">
              <Button variant="secondary" size="sm" onClick={() => startEdit(p)}>
                Edit
              </Button>
              {p.status && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => deactivateMutation.mutate(p.id)}
                  disabled={deactivateMutation.isPending}
                >
                  Deactivate
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
