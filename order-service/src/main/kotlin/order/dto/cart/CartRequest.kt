package order.dto.cart

data class AddCartItemRequest(
    val productId: Long,
    val quantity: Long
)
