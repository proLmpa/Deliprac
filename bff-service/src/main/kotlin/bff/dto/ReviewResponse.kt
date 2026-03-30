package bff.dto

data class ReviewResponse(
    val id: Long,
    val storeId: Long,
    val rating: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isOwner: Boolean
)
