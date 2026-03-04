package com.example.baemin.api.order

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.order.RevenueResponse
import com.example.baemin.service.order.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class StatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/api/stores/{storeId}/statistics/revenue")
    fun getRevenue(
        @PathVariable storeId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): RevenueResponse = statisticsService.getRevenue(storeId, year, month, currentUser())
}
