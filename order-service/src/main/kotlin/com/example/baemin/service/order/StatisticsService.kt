package com.example.baemin.service.order

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.repository.order.OrderRepository
import org.springframework.stereotype.Service
import java.time.ZoneId

@Service
class StatisticsService(private val orderRepository: OrderRepository) {

    fun getRevenue(storeId: Long, year: Int, month: Int, zoneId: ZoneId, principal: UserPrincipal): Int {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view revenue statistics")

        return orderRepository.sumRevenueByStoreAndMonth(storeId, year, month, zoneId)
    }

    fun getSpending(year: Int, month: Int, zoneId: ZoneId, principal: UserPrincipal): Int {
        return orderRepository.sumSpendingByUserAndMonth(principal.id, year, month, zoneId)
    }
}
