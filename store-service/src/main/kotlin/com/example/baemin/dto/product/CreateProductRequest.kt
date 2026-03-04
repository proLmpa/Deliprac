package com.example.baemin.dto.product

data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Int,
    val productPictureUrl: String?
)
