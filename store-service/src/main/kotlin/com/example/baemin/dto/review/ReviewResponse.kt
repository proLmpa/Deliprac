package com.example.baemin.dto.review

data class ReviewResponse(
    val id: Long,
    val storeId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(info: ReviewInfo) = ReviewResponse(
            id        = info.id,
            storeId   = info.storeId,
            userId    = info.userId,
            rating    = info.rating,
            content   = info.content,
            createdAt = info.createdAt,
            updatedAt = info.updatedAt
        )
    }
}