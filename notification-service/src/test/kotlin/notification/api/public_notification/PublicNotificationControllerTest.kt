package notification.api.public_notification

import common.exception.NotFoundException
import common.security.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import notification.config.SecurityConfig
import notification.dto.public_notification.CreatePublicNotificationRequest
import notification.dto.public_notification.DeactivatePublicNotificationRequest
import notification.dto.public_notification.PublicNotificationResponse
import notification.service.public_notification.PublicNotificationService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import notification.config.JwtProperties
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

@WebMvcTest(PublicNotificationController::class)
@Import(SecurityConfig::class)
@EnableConfigurationProperties(JwtProperties::class)
class PublicNotificationControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var publicNotificationService: PublicNotificationService
    @Autowired private lateinit var jwtProperties: JwtProperties

    private val notifId = 1L

    private fun bearerToken(role: UserRole = UserRole.ADMIN): String {
        val key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject("10")
            .claim("email", "admin@example.com")
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    private fun makeResponse(): PublicNotificationResponse {
        val now = System.currentTimeMillis()
        return PublicNotificationResponse(
            id        = notifId,
            title     = "Test Title",
            content   = "Test Content",
            isActive  = true,
            issuedAt  = now,
            expiresAt = now + 86_400_000L,
        )
    }

    // --- POST /api/public-notifications/list ---

    @Test
    fun `POST api public-notifications list - 200 with valid JWT`() {
        given(publicNotificationService.listActive()).willReturn(listOf(makeResponse()))

        mockMvc.perform(
            post("/api/public-notifications/list")
                .header("Authorization", bearerToken(UserRole.CUSTOMER))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(notifId))
            .andExpect(jsonPath("$[0].title").value("Test Title"))
            .andExpect(jsonPath("$[0].isActive").value(true))
    }

    @Test
    fun `POST api public-notifications list - 200 without JWT`() {
        given(publicNotificationService.listActive()).willReturn(emptyList())

        mockMvc.perform(post("/api/public-notifications/list"))
            .andExpect(status().isOk)
    }

    // --- POST /api/public-notifications ---

    @Test
    fun `POST api public-notifications - 201 with ADMIN JWT`() {
        val expiresAt = System.currentTimeMillis() + 86_400_000L
        val exactRequest = CreatePublicNotificationRequest(
            title     = "Test Title",
            content   = "Test Content",
            expiresAt = expiresAt,
        )
        given(publicNotificationService.create(exactRequest)).willReturn(makeResponse())

        mockMvc.perform(
            post("/api/public-notifications")
                .header("Authorization", bearerToken(UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Test Title","content":"Test Content","expiresAt":$expiresAt}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Test Title"))
    }

    @Test
    fun `POST api public-notifications - 403 with CUSTOMER JWT`() {
        mockMvc.perform(
            post("/api/public-notifications")
                .header("Authorization", bearerToken(UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"T","content":"C","expiresAt":9999999999999}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST api public-notifications - 403 without JWT`() {
        mockMvc.perform(
            post("/api/public-notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"T","content":"C","expiresAt":9999999999999}""")
        )
            .andExpect(status().isForbidden)
    }

    // --- PUT /api/public-notifications/deactivate ---

    @Test
    fun `PUT api public-notifications deactivate - 204 with ADMIN JWT`() {
        val exactRequest = DeactivatePublicNotificationRequest(notifId)
        willDoNothing().given(publicNotificationService).deactivate(exactRequest)

        mockMvc.perform(
            put("/api/public-notifications/deactivate")
                .header("Authorization", bearerToken(UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notifId}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT api public-notifications deactivate - 403 with CUSTOMER JWT`() {
        mockMvc.perform(
            put("/api/public-notifications/deactivate")
                .header("Authorization", bearerToken(UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notifId}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT api public-notifications deactivate - 404 when not found`() {
        val exactRequest = DeactivatePublicNotificationRequest(notifId)
        willThrow(NotFoundException("Public notification not found"))
            .given(publicNotificationService).deactivate(exactRequest)

        mockMvc.perform(
            put("/api/public-notifications/deactivate")
                .header("Authorization", bearerToken(UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationId":$notifId}""")
        )
            .andExpect(status().isNotFound)
    }
}
