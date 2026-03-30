package store.dto.review

import store.entity.review.Review

data class ReviewInfo(
    val id: Long,
    val storeId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(review: Review) = ReviewInfo(
            id        = review.id,
            storeId   = review.storeId,
            userId    = review.userId,
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
    val updatedAt: Long,
    val isOwner: Boolean
) {
    companion object {
        fun of(info: ReviewInfo, currentUserId: Long?) = ReviewResponse(
            id        = info.id,
            storeId   = info.storeId,
            rating    = info.rating,
            content   = info.content,
            createdAt = info.createdAt,
            updatedAt = info.updatedAt,
            isOwner   = currentUserId != null && info.userId == currentUserId
        )
    }
}
