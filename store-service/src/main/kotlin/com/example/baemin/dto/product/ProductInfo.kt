package com.example.baemin.dto.product

import com.example.baemin.entity.product.Product

data class ProductInfo(
    val id: Long,
    val storeId: Long,
    val name: String,
    val description: String,
    val price: Int,
    val productPictureUrl: String?,
    val popularity: Int,
    val status: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(product: Product) = ProductInfo(
            id                = product.id,
            storeId           = product.storeId,
            name              = product.name,
            description       = product.description,
            price             = product.price,
            productPictureUrl = product.productPictureUrl,
            popularity        = product.popularity,
            status            = product.status,
            createdAt         = product.createdAt,
            updatedAt         = product.updatedAt
        )
    }
}
