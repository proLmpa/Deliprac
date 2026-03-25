package bff.dto

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
)
