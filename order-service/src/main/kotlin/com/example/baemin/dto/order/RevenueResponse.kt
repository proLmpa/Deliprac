package com.example.baemin.dto.order

data class RevenueResponse(
    val storeId: Long,
    val year: Int,
    val month: Int,
    val totalRevenue: Int
)
