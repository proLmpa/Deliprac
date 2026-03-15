package order.dto.order

import order.entity.order.Order

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

data class RevenueResponse(
    val storeId: Long,
    val year: Int,
    val month: Int,
    val totalRevenue: Int
)

data class SpendingResponse(
    val year: Int,
    val month: Int,
    val totalSpending: Int
)
