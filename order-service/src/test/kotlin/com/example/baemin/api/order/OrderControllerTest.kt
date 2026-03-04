package com.example.baemin.api.order

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.config.SecurityConfig
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.dto.order.RevenueResponse
import com.example.baemin.dto.order.SpendingResponse
import com.example.baemin.service.order.OrderService
import com.example.baemin.service.order.StatisticsService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

@WebMvcTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var orderService: OrderService
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val ownerId  = 1L
    private val storeId  = 10L
    private val orderId  = 200L
    private val ownerPrincipal = UserPrincipal(ownerId, "owner@example.com", UserRole.OWNER)

    private fun bearerToken(userId: Long = ownerId, role: UserRole = UserRole.OWNER): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", "owner@example.com")
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    private val sampleOrder = OrderResponse(orderId, storeId, 8000, "PENDING", 0L, 0L)

    @Test
    fun `GET store orders - 200 with list`() {
        given(orderService.listByStore(storeId, ownerPrincipal)).willReturn(listOf(sampleOrder))

        mockMvc.perform(
            get("/api/stores/{storeId}/orders", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(orderId))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    @Test
    fun `GET store orders - 409 when non-OWNER`() {
        given(orderService.listByStore(storeId, ownerPrincipal))
            .willThrow(IllegalStateException("Only OWNER can view store orders"))

        mockMvc.perform(
            get("/api/stores/{storeId}/orders", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT mark sold - 200 with updated order`() {
        val sold = sampleOrder.copy(status = "SOLD")
        given(orderService.markSold(storeId, orderId, ownerPrincipal)).willReturn(sold)

        mockMvc.perform(
            put("/api/stores/{storeId}/orders/{orderId}/sold", storeId, orderId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SOLD"))
    }

    @Test
    fun `PUT mark sold - 409 when non-PENDING`() {
        given(orderService.markSold(storeId, orderId, ownerPrincipal))
            .willThrow(IllegalStateException("Order cannot be marked as sold"))

        mockMvc.perform(
            put("/api/stores/{storeId}/orders/{orderId}/sold", storeId, orderId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT mark cancel - 200 with updated order`() {
        val canceled = sampleOrder.copy(status = "CANCELED")
        given(orderService.markCanceled(storeId, orderId, ownerPrincipal)).willReturn(canceled)

        mockMvc.perform(
            put("/api/stores/{storeId}/orders/{orderId}/cancel", storeId, orderId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELED"))
    }
}

@WebMvcTest(UserOrderController::class)
@Import(SecurityConfig::class)
class UserOrderControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var orderService: OrderService
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val customerId = 2L
    private val customerPrincipal = UserPrincipal(customerId, "customer@example.com", UserRole.CUSTOMER)

    private fun bearerToken(): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(customerId.toString())
            .claim("email", "customer@example.com")
            .claim("role", UserRole.CUSTOMER.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    @Test
    fun `GET my orders - 200 with list`() {
        given(orderService.listByUser(customerPrincipal))
            .willReturn(listOf(OrderResponse(200L, 10L, 8000, "PENDING", 0L, 0L)))

        mockMvc.perform(
            get("/api/users/me/orders")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }
}

@WebMvcTest(StatisticsController::class)
@Import(SecurityConfig::class)
class StatisticsControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var statisticsService: StatisticsService
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val ownerId = 1L
    private val storeId = 10L
    private val ownerPrincipal = UserPrincipal(ownerId, "owner@example.com", UserRole.OWNER)

    private fun bearerToken(): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(ownerId.toString())
            .claim("email", "owner@example.com")
            .claim("role", UserRole.OWNER.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    @Test
    fun `GET revenue - 200 with revenue response`() {
        given(statisticsService.getRevenue(storeId, 2026, 3, ownerPrincipal))
            .willReturn(RevenueResponse(storeId, 2026, 3, 50000))

        mockMvc.perform(
            get("/api/stores/{storeId}/statistics/revenue", storeId)
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRevenue").value(50000))
            .andExpect(jsonPath("$.year").value(2026))
    }

    @Test
    fun `GET revenue - 409 when non-OWNER`() {
        given(statisticsService.getRevenue(storeId, 2026, 3, ownerPrincipal))
            .willThrow(IllegalStateException("Only OWNER can view revenue statistics"))

        mockMvc.perform(
            get("/api/stores/{storeId}/statistics/revenue", storeId)
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }
}

@WebMvcTest(UserStatisticsController::class)
@Import(SecurityConfig::class)
class UserStatisticsControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var statisticsService: StatisticsService
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val customerId = 2L
    private val customerPrincipal = UserPrincipal(customerId, "customer@example.com", UserRole.CUSTOMER)

    private fun bearerToken(): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(customerId.toString())
            .claim("email", "customer@example.com")
            .claim("role", UserRole.CUSTOMER.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    @Test
    fun `GET spending - 200 with spending response`() {
        given(statisticsService.getSpending(2026, 3, customerPrincipal))
            .willReturn(SpendingResponse(2026, 3, 24000))

        mockMvc.perform(
            get("/api/users/me/statistics/spending")
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalSpending").value(24000))
    }
}
