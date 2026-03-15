package order.repository.cart

import order.entity.cart.CartProduct
import org.springframework.data.jpa.repository.JpaRepository

interface CartProductRepository : JpaRepository<CartProduct, Long> {
    fun findAllByCartId(cartId: Long): List<CartProduct>
    fun findByCartIdAndProductId(cartId: Long, productId: Long): CartProduct?

    fun deleteByCartId(cartId: Long)

    fun deleteByCartIdAndProductId(cartId: Long, productId: Long)
}
