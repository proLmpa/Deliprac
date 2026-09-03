package common.event

data class OrderEventItem(
    val productId: Long,
    val productName: String,
    val quantity: Long,
    val unitPrice: Long
)
