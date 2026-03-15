package order.service.order

import order.client.StoreServiceClient
import common.orThrow
import common.security.UserRole
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.order.OrderRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartProductRepository: CartProductRepository,
    private val storeServiceClient: StoreServiceClient
) {

    @Transactional(readOnly = true)
    fun listByStore(storeId: Long, role: UserRole): List<Order> {
        if (role != UserRole.OWNER)
            throw IllegalStateException("Only OWNER can view store orders")

        return orderRepository.findAllByStoreId(storeId)
    }

    @Transactional
    fun markSold(storeId: Long, orderId: Long, role: UserRole): Order {
        if (role != UserRole.OWNER) throw IllegalStateException("Only OWNER can update orders")

        val order = orderRepository.findById(orderId).orThrow("Order not found")
        if (order.storeId != storeId) throw IllegalArgumentException("Order not found in this store")
        if (order.status != OrderStatus.PENDING) throw IllegalStateException("Order cannot be marked as sold")

        order.status    = OrderStatus.SOLD
        order.updatedAt = System.currentTimeMillis()
        val saved = orderRepository.save(order)

        cartProductRepository.findAllByCartId(order.cartId)
            .forEach { storeServiceClient.incrementPopularity(it.productId, it.quantity) }

        return saved
    }

    @Transactional
    fun markCanceled(storeId: Long, orderId: Long, role: UserRole): Order {
        if (role != UserRole.OWNER) throw IllegalStateException("Only OWNER can update orders")

        val order = orderRepository.findById(orderId).orThrow("Order not found")
        if (order.storeId != storeId) throw IllegalArgumentException("Order not found in this store")
        if (order.status != OrderStatus.PENDING) throw IllegalStateException("Order cannot be canceled")

        order.status    = OrderStatus.CANCELED
        order.updatedAt = System.currentTimeMillis()

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun listByUser(userId: Long): List<Order> =
        orderRepository.findAllByUserId(userId)

    @Transactional(readOnly = true)
    fun getById(orderId: Long): Order {
        return orderRepository.findById(orderId).orThrow("Order not found")
    }
}
