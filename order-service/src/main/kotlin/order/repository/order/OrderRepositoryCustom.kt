package order.repository.order

import java.time.ZoneId

interface OrderRepositoryCustom {
    fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int, zoneId: ZoneId): Long
    fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int, zoneId: ZoneId): Long
}
