package com.example.baemin.repository.review

import com.example.baemin.entity.review.Review
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findAllByStoreId(storeId: Long): List<Review>
}
