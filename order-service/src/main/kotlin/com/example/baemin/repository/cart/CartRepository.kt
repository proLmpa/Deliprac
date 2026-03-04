package com.example.baemin.repository.cart

import com.example.baemin.entity.cart.Cart
import org.springframework.data.jpa.repository.JpaRepository

interface CartRepository : JpaRepository<Cart, Long> {
    fun findByUserId(userId: Long): Cart?
}
