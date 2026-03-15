package order.api.order

import common.security.currentUser
import order.dto.order.SpendingRequest
import order.dto.order.SpendingResponse
import order.service.order.StatisticsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.ZoneId

@RestController
class UserStatisticsController(private val statisticsService: StatisticsService) {

    @PostMapping("/api/users/me/statistics/spending")
    fun getSpending(@RequestBody request: SpendingRequest): SpendingResponse {
        val zoneId = try { ZoneId.of(request.timezone) } catch (e: DateTimeException) { throw IllegalArgumentException("Invalid timezone: ${request.timezone}") }
        val totalSpending = statisticsService.getSpending(request.year, request.month, zoneId, currentUser().id)
        return SpendingResponse(year = request.year, month = request.month, totalSpending = totalSpending)
    }
}
