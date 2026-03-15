package order.service.order

import common.security.UserRole
import order.repository.order.OrderRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.ZoneId

@Service
class StatisticsService(private val orderRepository: OrderRepository) {

    @Transactional(readOnly = true)
    fun getRevenue(storeId: Long, year: Int, month: Int, zoneId: ZoneId, role: UserRole): Int {
        if (role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view revenue statistics")

        return orderRepository.sumRevenueByStoreAndMonth(storeId, year, month, zoneId)
    }

    @Transactional(readOnly = true)
    fun getSpending(year: Int, month: Int, zoneId: ZoneId, userId: Long): Int {
        return orderRepository.sumSpendingByUserAndMonth(userId, year, month, zoneId)
    }
}
