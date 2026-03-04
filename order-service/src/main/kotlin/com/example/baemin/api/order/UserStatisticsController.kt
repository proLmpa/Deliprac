package com.example.baemin.api.order

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.order.SpendingResponse
import com.example.baemin.service.order.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class UserStatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/api/users/me/statistics/spending")
    fun getSpending(
        @RequestParam year: Int,
        @RequestParam month: Int
    ): SpendingResponse = statisticsService.getSpending(year, month, currentUser())
}
