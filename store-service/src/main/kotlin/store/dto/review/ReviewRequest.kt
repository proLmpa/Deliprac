package store.dto.review

data class CreateReviewRequest(
    val rating: Int,
    val content: String
)

data class ListReviewRequest(val storeId: Long)
