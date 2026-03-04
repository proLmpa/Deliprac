package com.example.baemin.dto.cart

import com.example.baemin.entity.cart.CartProduct

data class CartProductResponse(
    val id: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Int
) {
    companion object {
        fun of(cartProduct: CartProduct) = CartProductResponse(
            id        = cartProduct.id,
            productId = cartProduct.productId,
            quantity  = cartProduct.quantity,
            unitPrice = cartProduct.unitPrice
        )
    }
}
