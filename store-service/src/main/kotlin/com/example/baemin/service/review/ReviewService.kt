package com.example.baemin.service.review

import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.review.CreateReviewRequest
import com.example.baemin.dto.review.ReviewInfo
import com.example.baemin.entity.review.Review
import com.example.baemin.repository.review.ReviewRepository
import com.example.baemin.repository.store.StoreRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository
) {

    @Transactional
    fun create(storeId: Long, request: CreateReviewRequest, principal: UserPrincipal): ReviewInfo {
        if (principal.role != UserRole.CUSTOMER) throw IllegalStateException("Only CUSTOMER can create reviews")
        if (request.rating < 1 || request.rating > 5) throw IllegalArgumentException("Rating must be between 1 and 5")
        storeRepository.findById(storeId).orThrow("Store not found")
        val now = System.currentTimeMillis()
        val review = Review(
            id        = 0L,
            storeId   = storeId,
            userId    = principal.id,
            rating    = request.rating,
            content   = request.content,
            createdAt = now,
            updatedAt = now
        )
        return ReviewInfo.of(reviewRepository.save(review))
    }

    @Transactional
    fun listByStore(storeId: Long): List<ReviewInfo> =
        reviewRepository.findAllByStoreId(storeId).map { ReviewInfo.of(it) }

    @Transactional
    fun delete(storeId: Long, reviewId: Long, principal: UserPrincipal) {
        val review = reviewRepository.findById(reviewId).orThrow("Review not found")
        if (review.storeId != storeId) throw IllegalArgumentException("Review not found in this store")
        if (review.userId != principal.id) throw IllegalStateException("Forbidden")
        reviewRepository.delete(review)
    }
}
