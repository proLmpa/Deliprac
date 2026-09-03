package bff.dto

data class AddToCartRequest(
    val productId: Long,
    val storeId: Long,
    val quantity: Long
)

data class RemoveCartItemRequest(val cartId: Long, val productId: Long)

data class ClearCartRequest(val cartId: Long)

data class CheckoutRequest(val cartId: Long)

data class CheckoutItemMeta(val productId: Long, val productName: String)

data class EnrichedCheckoutRequest(
    val cartId: Long,
    val storeOwnerId: Long,
    val storeName: String,
    val items: List<CheckoutItemMeta>
)