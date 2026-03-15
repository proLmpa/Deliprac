package order.api.order

import common.security.currentUser
import order.dto.order.SpendingResponse
import order.service.order.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.ZoneId

@RestController
class UserStatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/api/users/me/statistics/spending")
    fun getSpending(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(defaultValue = "UTC") timezone: String
    ): SpendingResponse {
        val zoneId = try { ZoneId.of(timezone) } catch (e: DateTimeException) { throw IllegalArgumentException("Invalid timezone: $timezone") }
        val totalSpending = statisticsService.getSpending(year, month, zoneId, currentUser().id)
        return SpendingResponse(year = year, month = month, totalSpending = totalSpending)
    }
}
