import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getCart, removeCartItem, clearCart, checkout } from '../../api/cart'
import { listProducts } from '../../api/products'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'

export default function Cart() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: cart, isLoading, error } = useQuery({
    queryKey: ['cart'],
    queryFn: getCart,
  })

  const { data: products } = useQuery({
    queryKey: ['products', cart?.storeId],
    queryFn: () => listProducts(cart!.storeId),
    enabled: !!cart?.storeId,
  })

  const productNameMap = Object.fromEntries(
    (products ?? []).map((p) => [p.id, p.name])
  )

  const removeMutation = useMutation({
    mutationFn: ({ cartId, productId }: { cartId: number; productId: number }) =>
      removeCartItem(cartId, productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
  })

  const clearMutation = useMutation({
    mutationFn: (cartId: number) => clearCart(cartId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
  })

  const checkoutMutation = useMutation({
    mutationFn: (cartId: number) => checkout(cartId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      navigate('/orders')
    },
  })

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading cart…</p>
  if (error || !cart) return <p className="text-center mt-10 text-gray-500">Your cart is empty.</p>

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">My Cart</h1>
      {cart.items.length === 0 ? (
        <p className="text-gray-500">Cart is empty.</p>
      ) : (
        <>
          <div className="space-y-3 mb-6">
            {cart.items.map((item) => (
              <Card key={item.id} className="flex justify-between items-center">
                <div>
                  <p className="font-medium">
                    {productNameMap[item.productId] ?? `Product #${item.productId}`}
                  </p>
                  <p className="text-sm text-gray-500">
                    ₩{item.unitPrice.toLocaleString()} × {item.quantity} ={' '}
                    <span className="font-semibold">
                      ₩{(item.unitPrice * item.quantity).toLocaleString()}
                    </span>
                  </p>
                </div>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => removeMutation.mutate({ cartId: cart.id, productId: item.productId })}
                  disabled={removeMutation.isPending}
                >
                  Remove
                </Button>
              </Card>
            ))}
          </div>
          <div className="bg-white rounded-lg shadow p-4 flex justify-between items-center">
            <p className="text-lg font-semibold">
              Total: ₩{cart.totalPrice.toLocaleString()}
            </p>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={() => clearMutation.mutate(cart.id)}
                disabled={clearMutation.isPending}
              >
                Clear Cart
              </Button>
              <Button
                onClick={() => checkoutMutation.mutate(cart.id)}
                disabled={checkoutMutation.isPending}
              >
                Checkout
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
