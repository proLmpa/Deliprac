package com.example.baemin.dto.cart

import com.example.baemin.entity.cart.Cart
import com.example.baemin.entity.cart.CartProduct

data class CartResponse(
    val id: Long,
    val storeId: Long,
    val items: List<CartProductResponse>,
    val totalPrice: Int
) {
    companion object {
        fun of(cart: Cart, items: List<CartProduct>) = CartResponse(
            id         = cart.id,
            storeId    = cart.storeId,
            items      = items.map { CartProductResponse.of(it) },
            totalPrice = items.sumOf { it.unitPrice * it.quantity }
        )
    }
}
