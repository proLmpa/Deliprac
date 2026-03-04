package com.example.baemin.dto.review

import com.example.baemin.entity.review.Review

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