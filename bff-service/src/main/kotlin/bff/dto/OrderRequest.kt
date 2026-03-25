package bff.dto

data class ListOrderRequest(val storeId: Long)

data class MarkOrderRequest(val storeId: Long, val orderId: Long)

data class RevenueRequest(val storeId: Long, val year: Int, val month: Int, val timezone: String = "UTC")

data class SpendingRequest(val year: Int, val month: Int, val timezone: String = "UTC")
