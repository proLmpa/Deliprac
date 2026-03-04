package com.example.baemin.dto.product

data class ProductResponse(
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
        fun of(info: ProductInfo) = ProductResponse(
            id                = info.id,
            storeId           = info.storeId,
            name              = info.name,
            description       = info.description,
            price             = info.price,
            productPictureUrl = info.productPictureUrl,
            popularity        = info.popularity,
            status            = info.status,
            createdAt         = info.createdAt,
            updatedAt         = info.updatedAt
        )
    }
}
