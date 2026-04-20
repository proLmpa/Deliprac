package notification.api

import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.security.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import notification.config.SecurityConfig
import notification.dto.CreateNotificationRequest
import notification.dto.NotificationResponse
import notification.entity.Notification
import notification.entity.NotificationType
import notification.service.NotificationService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
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
import java.util.Date

@WebMvcTest(NotificationController::class)
@Import(SecurityConfig::class)
class NotificationControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var notificationService: NotificationService
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val userId         = 1L
    private val notificationId = 10L

    private fun bearerToken(id: Long = userId, role: UserRole = UserRole.CUSTOMER): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(id.toString())
            .claim("email", "user@example.com")
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    private fun makeNotification(read: Boolean = false): NotificationResponse {
        val now = System.currentTimeMillis()
        return NotificationResponse(
            id        = notificationId,
            type      = NotificationType.NEW_ORDER,
            title     = "새 주문 접수",
            content   = "₩8000",
            storeId   = null,
            storeName = null,
            isRead    = read,
            issuedAt  = now,
            expiry    = now + Notification.MIN_EXPIRY_MILLIS + 1000L,
            createdAt = now,
            items     = emptyList()
        )
    }

    @Test
    fun `POST internal notifications - 201 no JWT required`() {
        val expiry = System.currentTimeMillis() + Notification.MIN_EXPIRY_MILLIS + 1000L
        val exactRequest = CreateNotificationRequest(
            recipientId = userId,
            type        = NotificationType.NEW_ORDER,
            title       = "새 주문 접수",
            content     = "₩8000",
            expiry      = expiry
        )
        given(notificationService.createNotification(exactRequest)).willReturn(makeNotification())

        mockMvc.perform(
            post("/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"recipientId":$userId,"type":"NEW_ORDER","title":"새 주문 접수","content":"₩8000","expiry":$expiry}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("새 주문 접수"))
            .andExpect(jsonPath("$.type").value("NEW_ORDER"))
    }

    @Test
    fun `POST api notifications list - 200 with valid JWT`() {
        given(notificationService.listMyNotifications(userId, false)).willReturn(listOf(makeNotification()))

        mockMvc.perform(
            post("/api/notifications/list")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unreadOnly":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(notificationId))
            .andExpect(jsonPath("$[0].title").value("새 주문 접수"))
            .andExpect(jsonPath("$[0].type").value("NEW_ORDER"))
    }

    @Test
    fun `POST api notifications list - 403 without JWT`() {
        mockMvc.perform(
            post("/api/notifications/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unreadOnly":false}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT api notifications read - 200 marks notification read`() {
        given(notificationService.markRead(userId, notificationId)).willReturn(makeNotification(read = true))

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notificationId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isRead").value(true))
    }

    @Test
    fun `PUT api notifications read - 403 when wrong user`() {
        given(notificationService.markRead(userId, notificationId))
            .willThrow(ForbiddenException("Forbidden"))

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notificationId}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT api notifications read - 404 when not found`() {
        given(notificationService.markRead(userId, notificationId))
            .willThrow(NotFoundException("Notification not found"))

        mockMvc.perform(
            put("/api/notifications/read")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notificationId}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT api notifications read-all - 204 marks all read`() {
        willDoNothing().given(notificationService).markAllRead(userId)

        mockMvc.perform(
            put("/api/notifications/read-all")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isNoContent)
    }
}
