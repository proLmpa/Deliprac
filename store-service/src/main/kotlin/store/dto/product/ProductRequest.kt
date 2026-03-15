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
