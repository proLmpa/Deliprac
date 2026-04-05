package order.dto.order

data class OrderCheckedOutEvent(val orderId: Long, val storeId: Long)
data class OrderMarkedSoldEvent(val orderId: Long, val customerId: Long)
data class OrderMarkedCanceledEvent(val orderId: Long, val customerId: Long)
