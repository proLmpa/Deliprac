package bff.dto

data class CartProductResponse(
    val id: Long,
    val productId: Long,
    val quantity: Long,
    val unitPrice: Long
)

data class CartResponse(
    val id: Long,
    val storeId: Long,
    val isOrdered: Boolean,
    val items: List<CartProductResponse>,
    val totalPrice: Long
)
