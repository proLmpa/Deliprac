package com.example.baemin.api.cart

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.config.SecurityConfig
import com.example.baemin.dto.cart.AddCartItemRequest
import com.example.baemin.dto.cart.CartProductResponse
import com.example.baemin.dto.cart.CartResponse
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.service.cart.CartService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.Date

@WebMvcTest(CartController::class)
@Import(SecurityConfig::class)
class CartControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var cartService: CartService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val userId  = 1L
    private val cartId  = 50L
    private val principal = UserPrincipal(userId, "user@example.com", UserRole.CUSTOMER)

    private fun bearerToken(userId: Long = this.userId, role: UserRole = UserRole.CUSTOMER): String {
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

    private val sampleCartResponse = CartResponse(
        id         = cartId,
        storeId    = 10L,
        items      = listOf(CartProductResponse(1L, 100L, 1, 8000)),
        totalPrice = 8000
    )

    private val sampleOrderResponse = OrderResponse(
        id         = 200L,
        storeId    = 10L,
        totalPrice = 8000,
        status     = "PENDING",
        createdAt  = 0L,
        updatedAt  = 0L
    )

    private val addRequest = AddCartItemRequest(productId = 100L, quantity = 1)

    @Test
    fun `POST carts - 200 with cart response`() {
        given(cartService.addItem(addRequest, principal)).willReturn(sampleCartResponse)

        mockMvc.perform(
            post("/api/carts")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(cartId))
            .andExpect(jsonPath("$.totalPrice").value(8000))
            .andExpect(jsonPath("$.items[0].productId").value(100))
    }

    @Test
    fun `POST carts - 400 when product not available`() {
        given(cartService.addItem(addRequest, principal))
            .willThrow(IllegalArgumentException("Product is not available"))

        mockMvc.perform(
            post("/api/carts")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Product is not available"))
    }

    @Test
    fun `GET carts - 200 with cart response`() {
        given(cartService.getMyCart(principal)).willReturn(sampleCartResponse)

        mockMvc.perform(
            get("/api/carts")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(cartId))
    }

    @Test
    fun `GET carts - 400 when cart not found`() {
        given(cartService.getMyCart(principal))
            .willThrow(IllegalArgumentException("Cart not found"))

        mockMvc.perform(
            get("/api/carts")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Cart not found"))
    }

    @Test
    fun `DELETE cart item - 204 no content`() {
        mockMvc.perform(
            delete("/api/carts/{cartId}/products/{productId}", cartId, 100L)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE cart - 204 no content`() {
        mockMvc.perform(
            delete("/api/carts/{cartId}", cartId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT checkout - 200 with order response`() {
        given(cartService.checkout(cartId, principal)).willReturn(sampleOrderResponse)

        mockMvc.perform(
            put("/api/carts/{cartId}/checkout", cartId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalPrice").value(8000))
    }

    @Test
    fun `PUT checkout - 409 when already checked out`() {
        given(cartService.checkout(cartId, principal))
            .willThrow(IllegalStateException("Cart already checked out"))

        mockMvc.perform(
            put("/api/carts/{cartId}/checkout", cartId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isConflict)
    }
}
