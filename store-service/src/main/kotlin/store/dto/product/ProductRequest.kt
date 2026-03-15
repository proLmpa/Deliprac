package store.dto.product

data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?
)

data class UpdateProductRequest(
    val name: String,
    val description: String,
    val price: Long,
    val productPictureUrl: String?
)

data class ListProductRequest(val storeId: Long)
data class FindProductRequest(val storeId: Long, val productId: Long)
data class PopularProductRequest(val storeId: Long)
data class FindInternalProductRequest(val productId: Long)
