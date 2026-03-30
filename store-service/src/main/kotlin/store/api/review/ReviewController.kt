package store.api.review

import common.security.currentUser
import common.security.optionalCurrentUser
import store.dto.review.CreateReviewRequest
import store.dto.review.DeleteReviewRequest
import store.dto.review.ListReviewRequest
import store.dto.review.ReviewResponse
import store.service.review.ReviewService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewController(private val reviewService: ReviewService) {

    @PostMapping("/api/stores/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateReviewRequest): ReviewResponse {
        val principal = currentUser()
        val review = reviewService.create(request.storeId, request, principal)
        return ReviewResponse.of(review, principal.id)
    }

    @PostMapping("/api/stores/reviews/list")
    fun listByStore(@RequestBody request: ListReviewRequest): List<ReviewResponse> {
        val currentUserId = optionalCurrentUser()?.id
        return reviewService.listByStore(request.storeId).map { ReviewResponse.of(it, currentUserId) }
    }

    @DeleteMapping("/api/stores/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestBody request: DeleteReviewRequest) {
        reviewService.delete(request.storeId, request.reviewId, currentUser())
    }
}
