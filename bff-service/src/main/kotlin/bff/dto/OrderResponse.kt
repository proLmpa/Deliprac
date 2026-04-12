package bff.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class OrderItemResponse(
    val productId: Long,
    val unitPrice: Long,
    val quantity: Long
)

data class OrderResponse(
    val id: Long,
    @get:JsonIgnore
    @param:JsonProperty("userId")
    val userId: Long = 0L,
    val storeId: Long,
    val totalPrice: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<OrderItemResponse> = emptyList()
)

data class RevenueResponse(
    val storeId: Long,
    val year: Int,
    val month: Int,
    val totalRevenue: Long
)

data class SpendingResponse(
    val year: Int,
    val month: Int,
    val totalSpending: Long
)
