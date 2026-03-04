package com.example.baemin.repository.cart

import com.example.baemin.entity.cart.CartProduct
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository

interface CartProductRepository : JpaRepository<CartProduct, Long> {
    fun findAllByCartId(cartId: Long): List<CartProduct>
    fun findByCartIdAndProductId(cartId: Long, productId: Long): CartProduct?

    @Transactional
    fun deleteByCartId(cartId: Long)

    @Transactional
    fun deleteByCartIdAndProductId(cartId: Long, productId: Long)
}
