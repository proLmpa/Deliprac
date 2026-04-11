package bff.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderResponse(
    val id: Long,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val userId: Long = 0L,
    val storeId: Long,
    val totalPrice: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
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
