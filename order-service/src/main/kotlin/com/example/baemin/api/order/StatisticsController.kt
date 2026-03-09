package com.example.baemin.api.order

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.order.RevenueResponse
import com.example.baemin.service.order.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.ZoneId

@RestController
class StatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/api/stores/{storeId}/statistics/revenue")
    fun getRevenue(
        @PathVariable storeId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(defaultValue = "UTC") timezone: String
    ): RevenueResponse {
        val zoneId = parseZoneId(timezone)
        val totalRevenue = statisticsService.getRevenue(storeId, year, month, zoneId, currentUser())
        return RevenueResponse(storeId = storeId, year = year, month = month, totalRevenue = totalRevenue)
    }

    private fun parseZoneId(timezone: String): ZoneId =
        try { ZoneId.of(timezone) } catch (e: DateTimeException) { throw IllegalArgumentException("Invalid timezone: $timezone") }
}
