package order.api.order

import common.security.currentUser
import order.dto.order.RevenueRequest
import order.dto.order.RevenueResponse
import order.service.order.StatisticsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.ZoneId

@RestController
class StatisticsController(private val statisticsService: StatisticsService) {

    @PostMapping("/api/stores/statistics/revenue")
    fun getRevenue(@RequestBody request: RevenueRequest): RevenueResponse {
        val zoneId = parseZoneId(request.timezone)
        val totalRevenue = statisticsService.getRevenue(request.storeId, request.year, request.month, zoneId, currentUser().role)
        return RevenueResponse(storeId = request.storeId, year = request.year, month = request.month, totalRevenue = totalRevenue)
    }

    private fun parseZoneId(timezone: String): ZoneId =
        try { ZoneId.of(timezone) } catch (e: DateTimeException) { throw IllegalArgumentException("Invalid timezone: $timezone") }
}
