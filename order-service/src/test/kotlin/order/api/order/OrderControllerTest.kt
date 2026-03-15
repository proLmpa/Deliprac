package order.api.order

import common.security.UserRole
import order.config.SecurityConfig
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.service.order.OrderService
import order.service.order.StatisticsService
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

    private fun makeOrder(status: OrderStatus = OrderStatus.PENDING) =
        Order(orderId, 0L, ownerId, storeId, 8000, status, 0L, 0L)

    @Test
    fun `GET store orders - 200 with list`() {
        given(orderService.listByStore(storeId, UserRole.OWNER)).willReturn(listOf(makeOrder()))

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
        given(orderService.listByStore(storeId, UserRole.OWNER))
            .willThrow(IllegalStateException("Only OWNER can view store orders"))

        mockMvc.perform(
            get("/api/stores/{storeId}/orders", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT mark sold - 200 with updated order`() {
        given(orderService.markSold(storeId, orderId, UserRole.OWNER)).willReturn(makeOrder(OrderStatus.SOLD))

        mockMvc.perform(
            put("/api/stores/{storeId}/orders/{orderId}/sold", storeId, orderId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SOLD"))
    }

    @Test
    fun `PUT mark sold - 409 when non-PENDING`() {
        given(orderService.markSold(storeId, orderId, UserRole.OWNER))
            .willThrow(IllegalStateException("Order cannot be marked as sold"))

        mockMvc.perform(
            put("/api/stores/{storeId}/orders/{orderId}/sold", storeId, orderId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT mark cancel - 200 with updated order`() {
        given(orderService.markCanceled(storeId, orderId, UserRole.OWNER)).willReturn(makeOrder(OrderStatus.CANCELED))

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
        given(orderService.listByUser(customerId))
            .willReturn(listOf(Order(200L, 0L, customerId, 10L, 8000, OrderStatus.PENDING, 0L, 0L)))

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
    private val utc = java.time.ZoneId.of("UTC")

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
        given(statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER)).willReturn(50000)

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
        given(statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER))
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
    private val utc = java.time.ZoneId.of("UTC")

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
        given(statisticsService.getSpending(2026, 3, utc, customerId)).willReturn(24000)

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
