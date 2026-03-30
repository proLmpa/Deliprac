package order.dto.order

data class ListOrderRequest(val storeId: Long)
data class RevenueRequest(val storeId: Long, val year: Int, val month: Int, val timezone: String? = null)
data class SpendingRequest(val year: Int, val month: Int, val timezone: String? = null)
data class FindOrderRequest(val orderId: Long)
data class MarkOrderRequest(val storeId: Long, val orderId: Long)
