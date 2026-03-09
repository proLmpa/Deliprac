package com.example.baemin.repository.order

import java.time.ZoneId

interface OrderRepositoryCustom {
    fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int, zoneId: ZoneId): Int
    fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int, zoneId: ZoneId): Int
}
