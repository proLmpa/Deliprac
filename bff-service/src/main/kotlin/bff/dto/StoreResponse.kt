package bff.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class StoreResponse(
    val id: Long,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val userId: Long = 0L,
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
