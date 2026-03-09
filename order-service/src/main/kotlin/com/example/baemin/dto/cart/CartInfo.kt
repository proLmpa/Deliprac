package com.example.baemin.dto.cart

import com.example.baemin.entity.cart.Cart
import com.example.baemin.entity.cart.CartProduct

data class CartInfo(
    val cart: Cart,
    val items: List<CartProduct>
)
