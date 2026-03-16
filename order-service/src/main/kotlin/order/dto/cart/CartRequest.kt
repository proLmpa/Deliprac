package order.dto.cart

data class AddCartItemRequest(
    val productId: Long,
    val storeId: Long,
    val unitPrice: Long,
    val quantity: Long
)

data class RemoveCartItemRequest(val cartId: Long, val productId: Long)
data class ClearCartRequest(val cartId: Long)
data class CheckoutRequest(val cartId: Long)
