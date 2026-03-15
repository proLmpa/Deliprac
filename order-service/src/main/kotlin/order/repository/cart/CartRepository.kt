package order.repository.cart

import order.entity.cart.Cart
import org.springframework.data.jpa.repository.JpaRepository

interface CartRepository : JpaRepository<Cart, Long> {
    fun findByUserId(userId: Long): Cart?
}
