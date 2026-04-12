package order.dto.order

import order.entity.cart.CartProduct
import order.entity.order.Order

data class OrderItemResponse(
    val productId: Long,
    val unitPrice: Long,
    val quantity: Long
)

data class OrderResponse(
    val id: Long,
    val userId: Long,
    val storeId: Long,
    val totalPrice: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<OrderItemResponse> = emptyList()
) {
    companion object {
        fun of(order: Order) = OrderResponse(
            id         = order.id,
            userId     = order.userId,
            storeId    = order.storeId,
            totalPrice = order.totalPrice,
            status     = order.status.name,
            createdAt  = order.createdAt,
            updatedAt  = order.updatedAt
        )

        fun of(order: Order, cartProducts: List<CartProduct>) = OrderResponse(
            id         = order.id,
            userId     = order.userId,
            storeId    = order.storeId,
            totalPrice = order.totalPrice,
            status     = order.status.name,
            createdAt  = order.createdAt,
            updatedAt  = order.updatedAt,
            items      = cartProducts.map { OrderItemResponse(it.productId, it.unitPrice, it.quantity) }
        )
    }
}

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
