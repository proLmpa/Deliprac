package store.api.review

import common.security.UserPrincipal
import common.security.UserRole
import store.config.SecurityConfig
import store.dto.review.CreateReviewRequest
import store.dto.review.ReviewInfo
import store.service.review.ReviewService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.Date

@WebMvcTest(ReviewController::class)
@Import(SecurityConfig::class)
class ReviewControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var reviewService: ReviewService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val customerId        = 1L
    private val storeId           = 10L
    private val reviewId          = 100L
    private val customerPrincipal = UserPrincipal(customerId, UserRole.CUSTOMER)

    private fun bearerToken(
        userId: Long = customerId,
        email: String = "customer@example.com",
        role: UserRole = UserRole.CUSTOMER
    ): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()
        return "Bearer $token"
    }

    private val sampleInfo = ReviewInfo(
        id = reviewId, storeId = storeId,
        rating = 5, content = "Great food!", createdAt = 0L, updatedAt = 0L
    )

    private val createRequest = CreateReviewRequest(rating = 5, content = "Great food!")

    @Test
    fun `POST reviews - 201 with review response`() {
        given(reviewService.create(storeId, createRequest, customerPrincipal)).willReturn(sampleInfo)

        mockMvc.perform(
            post("/api/stores/{storeId}/reviews", storeId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(reviewId))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.content").value("Great food!"))
    }

    @Test
    fun `POST reviews - 409 when non-customer tries to create`() {
        given(reviewService.create(storeId, createRequest, customerPrincipal))
            .willThrow(IllegalStateException("Only CUSTOMER can create reviews"))

        mockMvc.perform(
            post("/api/stores/{storeId}/reviews", storeId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST reviews - 400 when store not found`() {
        given(reviewService.create(storeId, createRequest, customerPrincipal))
            .willThrow(IllegalArgumentException("Store not found"))

        mockMvc.perform(
            post("/api/stores/{storeId}/reviews", storeId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Store not found"))
    }

    @Test
    fun `POST reviews list - 200 with list`() {
        given(reviewService.listByStore(storeId)).willReturn(listOf(sampleInfo))

        mockMvc.perform(
            post("/api/stores/reviews/list")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(reviewId))
            .andExpect(jsonPath("$[0].rating").value(5))
    }

    @Test
    fun `DELETE review - 204 no content`() {
        mockMvc.perform(
            delete("/api/stores/{storeId}/reviews/{reviewId}", storeId, reviewId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE review - 409 when wrong user`() {
        given(reviewService.delete(storeId, reviewId, customerId))
            .willThrow(IllegalStateException("Forbidden"))

        mockMvc.perform(
            delete("/api/stores/{storeId}/reviews/{reviewId}", storeId, reviewId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `DELETE review - 400 when review not found`() {
        given(reviewService.delete(storeId, reviewId, customerId))
            .willThrow(IllegalArgumentException("Review not found"))

        mockMvc.perform(
            delete("/api/stores/{storeId}/reviews/{reviewId}", storeId, reviewId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Review not found"))
    }
}
