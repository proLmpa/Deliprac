package com.example.baemin.service.order

import com.example.baemin.client.StoreServiceClient
import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.entity.order.Order
import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.repository.cart.CartProductRepository
import com.example.baemin.repository.order.OrderRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartProductRepository: CartProductRepository,
    private val storeServiceClient: StoreServiceClient
) {

    @Transactional
    fun listByStore(storeId: Long, principal: UserPrincipal): List<Order> {
        if (principal.role != UserRole.OWNER)
            throw IllegalStateException("Only OWNER can view store orders")

        return orderRepository.findAllByStoreId(storeId)
    }

    @Transactional
    fun markSold(storeId: Long, orderId: Long, principal: UserPrincipal): Order {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can update orders")

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
    fun markCanceled(storeId: Long, orderId: Long, principal: UserPrincipal): Order {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can update orders")

        val order = orderRepository.findById(orderId).orThrow("Order not found")
        if (order.storeId != storeId) throw IllegalArgumentException("Order not found in this store")
        if (order.status != OrderStatus.PENDING) throw IllegalStateException("Order cannot be canceled")

        order.status    = OrderStatus.CANCELED
        order.updatedAt = System.currentTimeMillis()

        return orderRepository.save(order)
    }

    @Transactional
    fun listByUser(principal: UserPrincipal): List<Order> =
        orderRepository.findAllByUserId(principal.id)

    @Transactional
    fun getById(orderId: Long): Order {
        return orderRepository.findById(orderId).orThrow("Order not found")
    }
}
