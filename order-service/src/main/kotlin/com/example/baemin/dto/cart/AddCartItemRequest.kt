package com.example.baemin.dto.cart

data class AddCartItemRequest(
    val productId: Long,
    val quantity: Int
)
