package com.example.baemin.api.review

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.review.CreateReviewRequest
import com.example.baemin.dto.review.ReviewResponse
import com.example.baemin.service.review.ReviewService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewController(private val reviewService: ReviewService) {

    @PostMapping("/api/stores/{storeId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable storeId: Long,
        @RequestBody request: CreateReviewRequest
    ): ReviewResponse = ReviewResponse.of(reviewService.create(storeId, request, currentUser()))

    @GetMapping("/api/stores/{storeId}/reviews")
    fun listByStore(@PathVariable storeId: Long): List<ReviewResponse> =
        reviewService.listByStore(storeId).map { ReviewResponse.of(it) }

    @DeleteMapping("/api/stores/{storeId}/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable storeId: Long,
        @PathVariable reviewId: Long
    ) = reviewService.delete(storeId, reviewId, currentUser())
}
