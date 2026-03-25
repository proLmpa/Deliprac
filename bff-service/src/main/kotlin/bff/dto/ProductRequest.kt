package bff.dto

data class CreateProductRequest(
    val storeId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?
)

data class UpdateProductRequest(
    val storeId: Long,
    val productId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?
)

data class DeactivateProductRequest(val storeId: Long, val productId: Long)

data class IncrementPopularityRequest(val storeId: Long, val productId: Long, val delta: Long)

data class ListProductRequest(val storeId: Long)

data class FindProductRequest(val storeId: Long, val productId: Long)

data class PopularProductRequest(val storeId: Long)
