package notification.api

import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.security.UserPrincipal
import common.security.UserRole
import notification.config.SecurityConfig
import notification.dto.MarkReadRequest
import notification.dto.NotificationResponse
import notification.service.NotificationService
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

@WebMvcTest(NotificationController::class)
@Import(SecurityConfig::class)
class NotificationControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var notificationService: NotificationService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val userId    = 1L
    private val principal = UserPrincipal(userId, UserRole.CUSTOMER)

    private fun bearerToken(
        userId: Long = this.userId,
        role: UserRole = UserRole.CUSTOMER
    ): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", "user@example.com")
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    private val sampleResponse = NotificationResponse(
        id        = 100L,
        title     = "New Order",
        content   = "A new order has arrived.",
        isRead    = false,
        createdAt = 0L
    )

    @Test
    fun `POST notifications me - 200 with notification list`() {
        given(notificationService.listByUser(userId)).willReturn(listOf(sampleResponse))

        mockMvc.perform(
            post("/api/notifications/me")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(100L))
            .andExpect(jsonPath("$[0].title").value("New Order"))
            .andExpect(jsonPath("$[0].isRead").value(false))
    }

    @Test
    fun `POST notifications me - 401 without token`() {
        mockMvc.perform(post("/api/notifications/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT notifications read - 200 ok`() {
        val request = MarkReadRequest(notificationId = 100L)

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT notifications read - 404 when notification not found`() {
        val request = MarkReadRequest(notificationId = 100L)
        given(notificationService.markRead(100L, userId)).willThrow(NotFoundException("Notification not found"))

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Notification not found"))
    }

    @Test
    fun `PUT notifications read - 403 when wrong user`() {
        val request = MarkReadRequest(notificationId = 100L)
        given(notificationService.markRead(100L, userId)).willThrow(ForbiddenException("Forbidden"))

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }
}
