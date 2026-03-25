package bff.dto

data class CreateStoreRequest(
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val storePictureUrl: String?,
    val productCreatedTime: Long,
    val openedTime: Long,
    val closedTime: Long,
    val closedDays: String
)

data class UpdateStoreRequest(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val storePictureUrl: String?,
    val productCreatedTime: Long,
    val openedTime: Long,
    val closedTime: Long,
    val closedDays: String
)

data class DeactivateStoreRequest(val id: Long)

data class ListStoreRequest(val sortBy: String = "CREATED_AT")

data class FindStoreRequest(val id: Long)
