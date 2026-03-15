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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.Date

@WebMvcTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var orderService: OrderService
    @Autowired private lateinit var objectMapper: ObjectMapper
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
        Order(orderId, 0L, ownerId, storeId, 8000L, status, 0L, 0L)

    @Test
    fun `POST store orders list - 200 with list`() {
        given(orderService.listByStore(storeId, UserRole.OWNER)).willReturn(listOf(makeOrder()))

        mockMvc.perform(
            post("/api/stores/orders/list")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(orderId))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    @Test
    fun `POST store orders list - 409 when non-OWNER`() {
        given(orderService.listByStore(storeId, UserRole.OWNER))
            .willThrow(IllegalStateException("Only OWNER can view store orders"))

        mockMvc.perform(
            post("/api/stores/orders/list")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
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
    fun `POST my orders - 200 with list`() {
        given(orderService.listByUser(customerId))
            .willReturn(listOf(Order(200L, 0L, customerId, 10L, 8000L, OrderStatus.PENDING, 0L, 0L)))

        mockMvc.perform(
            post("/api/users/me/orders")
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
    @Autowired private lateinit var objectMapper: ObjectMapper
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
    fun `POST revenue - 200 with revenue response`() {
        given(statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER)).willReturn(50000L)

        mockMvc.perform(
            post("/api/stores/statistics/revenue")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"year":2026,"month":3}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRevenue").value(50000))
            .andExpect(jsonPath("$.year").value(2026))
    }

    @Test
    fun `POST revenue - 409 when non-OWNER`() {
        given(statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER))
            .willThrow(IllegalStateException("Only OWNER can view revenue statistics"))

        mockMvc.perform(
            post("/api/stores/statistics/revenue")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"year":2026,"month":3}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST revenue - 400 when invalid timezone`() {
        mockMvc.perform(
            post("/api/stores/statistics/revenue")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"year":2026,"month":3,"timezone":"INVALID"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid timezone: INVALID"))
    }

    @Test
    fun `POST revenue - 200 with non-UTC timezone`() {
        val seoul = java.time.ZoneId.of("Asia/Seoul")
        given(statisticsService.getRevenue(storeId, 2026, 3, seoul, UserRole.OWNER)).willReturn(30000L)

        mockMvc.perform(
            post("/api/stores/statistics/revenue")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"year":2026,"month":3,"timezone":"Asia/Seoul"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRevenue").value(30000))
    }
}

@WebMvcTest(UserStatisticsController::class)
@Import(SecurityConfig::class)
class UserStatisticsControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var statisticsService: StatisticsService
    @Autowired private lateinit var objectMapper: ObjectMapper
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
    fun `POST spending - 200 with spending response`() {
        given(statisticsService.getSpending(2026, 3, utc, customerId)).willReturn(24000L)

        mockMvc.perform(
            post("/api/users/me/statistics/spending")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"year":2026,"month":3}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalSpending").value(24000))
    }

    @Test
    fun `POST spending - 400 when invalid timezone`() {
        mockMvc.perform(
            post("/api/users/me/statistics/spending")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"year":2026,"month":3,"timezone":"INVALID"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid timezone: INVALID"))
    }

    @Test
    fun `POST spending - 200 with non-UTC timezone`() {
        val seoul = java.time.ZoneId.of("Asia/Seoul")
        given(statisticsService.getSpending(2026, 3, seoul, customerId)).willReturn(15000L)

        mockMvc.perform(
            post("/api/users/me/statistics/spending")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"year":2026,"month":3,"timezone":"Asia/Seoul"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalSpending").value(15000))
    }
}
