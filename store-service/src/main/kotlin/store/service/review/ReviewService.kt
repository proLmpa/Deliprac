package store.service.review

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.orThrow
import common.security.UserPrincipal
import common.security.UserRole
import store.dto.review.CreateReviewRequest
import store.dto.review.ReviewInfo
import store.entity.review.Review
import store.repository.review.ReviewRepository
import store.repository.store.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository
) {

    @Transactional
    fun create(storeId: Long, request: CreateReviewRequest, principal: UserPrincipal): ReviewInfo {
        if (principal.role != UserRole.CUSTOMER) throw ForbiddenException("Only CUSTOMER can create reviews")
        if (request.rating !in 1 .. 5) throw IllegalArgumentException("Rating must be between 1 and 5")

        storeRepository.findById(storeId).orThrow("Store not found")

        val review = Review(
            id      = 0L,
            storeId = storeId,
            userId  = principal.id,
            rating  = request.rating,
            content = request.content,
        )

        return ReviewInfo.of(reviewRepository.save(review))
    }

    @Transactional
    fun listByStore(storeId: Long): List<ReviewInfo> =
        reviewRepository.findAllByStoreId(storeId).map { ReviewInfo.of(it) }

    @Transactional
    fun delete(storeId: Long, reviewId: Long, principal: UserPrincipal) {
        val review = reviewRepository.findById(reviewId).orThrow("Review not found")
        if (review.storeId != storeId) throw NotFoundException("Review not found in this store")
        if ((review.userId != principal.id) and (principal.role != UserRole.ADMIN)) throw ForbiddenException("Forbidden")

        reviewRepository.delete(review)
    }
}
