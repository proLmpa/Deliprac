package order.repository.order

import order.entity.order.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long>, OrderRepositoryCustom {
    fun findAllByStoreId(storeId: Long): List<Order>
    fun findAllByUserId(userId: Long): List<Order>
    fun existsByCartId(cartId: Long): Boolean
}
