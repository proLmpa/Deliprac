package com.example.baemin.service.order

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.order.RevenueResponse
import com.example.baemin.dto.order.SpendingResponse
import com.example.baemin.repository.order.OrderRepository
import org.springframework.stereotype.Service

@Service
class StatisticsService(private val orderRepository: OrderRepository) {

    fun getRevenue(storeId: Long, year: Int, month: Int, principal: UserPrincipal): RevenueResponse {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view revenue statistics")
        val totalRevenue = orderRepository.sumRevenueByStoreAndMonth(storeId, year, month)
        return RevenueResponse(storeId = storeId, year = year, month = month, totalRevenue = totalRevenue)
    }

    fun getSpending(year: Int, month: Int, principal: UserPrincipal): SpendingResponse {
        val totalSpending = orderRepository.sumSpendingByUserAndMonth(principal.id, year, month)
        return SpendingResponse(year = year, month = month, totalSpending = totalSpending)
    }
}
