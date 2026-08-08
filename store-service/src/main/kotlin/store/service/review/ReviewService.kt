package store.service.review

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
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val auditLog = LoggerFactory.getLogger("audit")

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository
) {

    @Transactional
    fun create(storeId: Long, request: CreateReviewRequest, principal: UserPrincipal): ReviewInfo {
        if (principal.role != UserRole.CUSTOMER) throw ForbiddenException("Forbidden")
        if (request.rating !in 1 .. 5) throw IllegalArgumentException("Invalid request")

        storeRepository.findById(storeId).orThrow("Not found")

        val review = Review(
            id      = 0L,
            storeId = storeId,
            userId  = principal.id,
            rating  = request.rating,
            content = request.content,
        )

        val saved = reviewRepository.save(review)
        auditLog.info(appendEntries(mapOf("event" to "REVIEW_CREATED", "reviewId" to saved.id, "storeId" to storeId, "userId" to principal.id, "rating" to request.rating)), "REVIEW_CREATED")
        return ReviewInfo.of(saved)
    }

    @Transactional(readOnly = true)
    fun listByStore(storeId: Long): List<ReviewInfo> =
        reviewRepository.findAllByStoreId(storeId).map { ReviewInfo.of(it) }

    @Transactional
    fun delete(storeId: Long, reviewId: Long, principal: UserPrincipal) {
        val review = reviewRepository.findById(reviewId).orThrow("Not found")
        if (review.storeId != storeId) throw NotFoundException("Not found")
        if ((review.userId != principal.id) and (principal.role != UserRole.ADMIN)) throw ForbiddenException("Forbidden")

        reviewRepository.delete(review)
    }
}
