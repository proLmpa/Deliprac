package com.example.baemin.dto.review

data class CreateReviewRequest(
    val rating: Int,
    val content: String
)