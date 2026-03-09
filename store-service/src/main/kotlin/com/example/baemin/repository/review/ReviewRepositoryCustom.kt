package com.example.baemin.repository.review

interface ReviewRepositoryCustom {
    fun calculateAverageRatingByStoreId(storeId: Long): Double
    fun calculateAverageRatingsForStores(storeIds: List<Long>): Map<Long, Double>
}
