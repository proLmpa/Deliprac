package store.repository.review

import store.entity.review.Review
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long>, ReviewRepositoryCustom {
    fun findAllByStoreId(storeId: Long): List<Review>
}
