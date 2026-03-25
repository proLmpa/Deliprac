package bff.dto

// unitPrice is intentionally omitted — the BFF fetches it from store-service
data class AddToCartRequest(
    val productId: Long,
    val storeId: Long,
    val quantity: Long
)

data class RemoveCartItemRequest(val cartId: Long, val productId: Long)

data class ClearCartRequest(val cartId: Long)

data class CheckoutRequest(val cartId: Long)
