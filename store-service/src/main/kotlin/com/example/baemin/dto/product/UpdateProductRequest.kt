package com.example.baemin.dto.product

data class UpdateProductRequest(
    val name: String,
    val description: String,
    val price: Int,
    val productPictureUrl: String?
)
