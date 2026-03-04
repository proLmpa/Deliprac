package com.example.baemin.dto.order

import com.example.baemin.entity.order.Order

data class OrderResponse(
    val id: Long,
    val storeId: Long,
    val totalPrice: Int,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(order: Order) = OrderResponse(
            id         = order.id,
            storeId    = order.storeId,
            totalPrice = order.totalPrice,
            status     = order.status.name,
            createdAt  = order.createdAt,
            updatedAt  = order.updatedAt
        )
    }
}
