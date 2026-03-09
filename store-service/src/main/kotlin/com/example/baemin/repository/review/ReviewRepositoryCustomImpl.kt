package com.example.baemin.repository.review

import com.example.baemin.entity.review.QReview
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ReviewRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ReviewRepositoryCustom {

    override fun calculateAverageRatingByStoreId(storeId: Long): Double {
        val review = QReview.review
        return queryFactory
            .select(review.rating.avg())
            .from(review)
            .where(review.storeId.eq(storeId))
            .fetchOne() ?: 0.0
    }

    override fun calculateAverageRatingsForStores(storeIds: List<Long>): Map<Long, Double> {
        if (storeIds.isEmpty()) return emptyMap()
        val review = QReview.review
        val avgExpr = review.rating.avg()
        return queryFactory
            .select(review.storeId, avgExpr)
            .from(review)
            .where(review.storeId.`in`(storeIds))
            .groupBy(review.storeId)
            .fetch()
            .associate { it.get(review.storeId)!! to (it.get(avgExpr) ?: 0.0) }
    }
}