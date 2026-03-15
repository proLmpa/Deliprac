package store.dto.product

import store.entity.product.Product

data class ProductInfo(
    val id: Long,
    val storeId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?,
    val popularity: Long,
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

data class ProductResponse(
    val id: Long,
    val storeId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?,
    val popularity: Long,
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
