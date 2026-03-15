package store.dto.review

import store.entity.review.Review

data class ReviewInfo(
    val id: Long,
    val storeId: Long,
    val rating: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(review: Review) = ReviewInfo(
            id        = review.id,
            storeId   = review.storeId,
            rating    = review.rating,
            content   = review.content,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt
        )
    }
}

data class ReviewResponse(
    val id: Long,
    val storeId: Long,
    val rating: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(info: ReviewInfo) = ReviewResponse(
            id        = info.id,
            storeId   = info.storeId,
            rating    = info.rating,
            content   = info.content,
            createdAt = info.createdAt,
            updatedAt = info.updatedAt
        )
    }
}
