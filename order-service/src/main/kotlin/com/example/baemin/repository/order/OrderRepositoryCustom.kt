package com.example.baemin.repository.order

interface OrderRepositoryCustom {
    fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int): Int
    fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int): Int
}
