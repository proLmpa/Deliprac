package bff.dto

import com.fasterxml.jackson.annotation.JsonIgnore

data class OrderResponse(
    val id: Long,
    @get:JsonIgnore
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
