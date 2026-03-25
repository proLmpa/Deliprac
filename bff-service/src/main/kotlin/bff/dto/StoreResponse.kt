package bff.dto

data class StoreResponse(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val status: String,
    val storePictureUrl: String?,
    val productCreatedTime: Long,
    val openedTime: Long,
    val closedTime: Long,
    val closedDays: String,
    val averageRating: Double,
    val createdAt: Long,
    val updatedAt: Long
)
