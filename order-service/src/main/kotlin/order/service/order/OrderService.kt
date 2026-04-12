package order.service.order

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.orThrow
import common.security.UserRole
import order.dto.order.OrderResponse
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
) {

    @Transactional(readOnly = true)
    fun listByStore(storeId: Long, role: UserRole): List<Order> {
        if (role != UserRole.OWNER)
            throw ForbiddenException("Only OWNER can view store orders")

        return orderRepository.findAllByStoreId(storeId)
    }

    @Transactional
    fun markSold(storeId: Long, orderId: Long, role: UserRole): OrderResponse {
        if (role != UserRole.OWNER) throw ForbiddenException("Only OWNER can update orders")

        val order = orderRepository.findById(orderId).orThrow("Order not found")
        if (order.storeId != storeId) throw NotFoundException("Order not found in this store")
        if (order.status != OrderStatus.PENDING) throw ConflictException("Order cannot be marked as sold")

        order.status = OrderStatus.SOLD
        val saved = orderRepository.save(order)
        val items = cartProductRepository.findAllByCartId(saved.cartId)
        return OrderResponse.of(saved, items)
    }

    @Transactional
    fun markCanceled(storeId: Long, orderId: Long, role: UserRole): OrderResponse {
        if (role != UserRole.OWNER) throw ForbiddenException("Only OWNER can update orders")

        val order = orderRepository.findById(orderId).orThrow("Order not found")
        if (order.storeId != storeId) throw NotFoundException("Order not found in this store")
        if (order.status != OrderStatus.PENDING) throw ConflictException("Order cannot be canceled")

        order.status = OrderStatus.CANCELED
        val saved = orderRepository.save(order)
        val items = cartProductRepository.findAllByCartId(saved.cartId)
        return OrderResponse.of(saved, items)
    }

    @Transactional(readOnly = true)
    fun listByUser(userId: Long): List<Order> =
        orderRepository.findAllByUserId(userId)

    @Transactional(readOnly = true)
    fun getById(orderId: Long): Order {
        return orderRepository.findById(orderId).orThrow("Order not found")
    }
}
