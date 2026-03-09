package com.example.baemin.service.order

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.repository.order.OrderRepository
import org.springframework.stereotype.Service

@Service
class StatisticsService(private val orderRepository: OrderRepository) {

    fun getRevenue(storeId: Long, year: Int, month: Int, principal: UserPrincipal): Int {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view revenue statistics")

        return orderRepository.sumRevenueByStoreAndMonth(storeId, year, month)
    }

    fun getSpending(year: Int, month: Int, principal: UserPrincipal): Int {
        return orderRepository.sumSpendingByUserAndMonth(principal.id, year, month)
    }
}
