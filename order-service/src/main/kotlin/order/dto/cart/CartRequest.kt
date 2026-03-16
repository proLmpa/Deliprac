package order.dto.cart

data class AddCartItemRequest(
    val productId: Long,
    val storeId: Long,
    val unitPrice: Long,
    val quantity: Long
)
