package order.dto.cart

data class AddCartItemRequest(
    val productId: Long,
    val storeId: Long,
    val unitPrice: Long,
    val quantity: Long
)

data class RemoveCartItemRequest(val cartId: Long, val productId: Long)
data class ClearCartRequest(val cartId: Long)
data class CheckoutItemMeta(val productId: Long, val productName: String)

data class CheckoutRequest(
    val cartId: Long,
    val storeOwnerId: Long,
    val storeName: String,
    val items: List<CheckoutItemMeta>
)