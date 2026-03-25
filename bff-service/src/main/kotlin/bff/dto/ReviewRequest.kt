package bff.dto

data class CreateReviewRequest(
    val storeId: Long,
    val rating: Int,
    val content: String
)

data class DeleteReviewRequest(val storeId: Long, val reviewId: Long)

data class ListReviewRequest(val storeId: Long)
