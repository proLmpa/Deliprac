package common.event

data class OrderEvent (
    val eventType: OrderEventType,
    val orderId: Long,
    val userId: Long,
    val storeId: Long,
    val storeOwnerId: Long,
    val storeName: String,
    val totalPrice: Long,
    val items: List<OrderEventItem>,
    val occurredAt: Long = System.currentTimeMillis()
)