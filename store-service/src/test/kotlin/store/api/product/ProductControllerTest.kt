package store.api.product

import common.security.UserPrincipal
import common.security.UserRole
import common.exception.ForbiddenException
import common.exception.NotFoundException
import store.config.SecurityConfig
import store.dto.product.CreateProductRequest
import store.dto.product.ProductInfo
import store.dto.product.UpdateProductRequest
import store.service.product.ProductService
import store.service.product.ProductStatisticsService
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

@WebMvcTest(ProductController::class)
@Import(SecurityConfig::class)
class ProductControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var productService: ProductService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val ownerId   = 1L
    private val storeId   = 10L
    private val productId = 100L
    private val ownerPrincipal = UserPrincipal(ownerId, UserRole.OWNER)

    private fun bearerToken(
        userId: Long = ownerId,
        email: String = "owner@example.com",
        role: UserRole = UserRole.OWNER
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

    private val sampleInfo = ProductInfo(
        id = productId, storeId = storeId, name = "Burger", description = "Tasty burger",
        price = 8000L, productPictureUrl = null, popularity = 0L, status = true,
        createdAt = 0L, updatedAt = 0L
    )

    private val createRequest = CreateProductRequest(
        storeId = storeId, name = "Burger", description = "Tasty burger", price = 8000L, productPictureUrl = null
    )

    private val updateRequest = UpdateProductRequest(
        storeId = storeId, productId = productId,
        name = "Updated Burger", description = "Even tastier", price = 9000L, productPictureUrl = null
    )

    @Test
    fun `POST products - 201 with product response`() {
        given(productService.create(storeId, createRequest, ownerPrincipal)).willReturn(sampleInfo)

        mockMvc.perform(
            post("/api/stores/products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(productId))
            .andExpect(jsonPath("$.name").value("Burger"))
            .andExpect(jsonPath("$.price").value(8000))
    }

    @Test
    fun `POST products - 409 when wrong owner`() {
        given(productService.create(storeId, createRequest, ownerPrincipal))
            .willThrow(ForbiddenException("Forbidden"))

        mockMvc.perform(
            post("/api/stores/products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST products list - 200 with list`() {
        given(productService.listByStore(storeId)).willReturn(listOf(sampleInfo))

        mockMvc.perform(
            post("/api/stores/products/list")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(productId))
            .andExpect(jsonPath("$[0].name").value("Burger"))
    }

    @Test
    fun `POST products find - 200 with product response`() {
        given(productService.findById(storeId, productId)).willReturn(sampleInfo)

        mockMvc.perform(
            post("/api/stores/products/find")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"productId":$productId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(productId))
            .andExpect(jsonPath("$.price").value(8000))
    }

    @Test
    fun `POST products find - 400 when not found`() {
        given(productService.findById(storeId, productId))
            .willThrow(NotFoundException("Product not found"))

        mockMvc.perform(
            post("/api/stores/products/find")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"productId":$productId}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Product not found"))
    }

    @Test
    fun `PUT products - 200 with updated product`() {
        val updated = sampleInfo.copy(name = "Updated Burger", price = 9000L)
        given(productService.update(storeId, productId, updateRequest, ownerId)).willReturn(updated)

        mockMvc.perform(
            put("/api/stores/products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Burger"))
            .andExpect(jsonPath("$.price").value(9000))
    }

    @Test
    fun `PUT products - 409 when wrong owner`() {
        given(productService.update(storeId, productId, updateRequest, ownerId))
            .willThrow(ForbiddenException("Forbidden"))

        mockMvc.perform(
            put("/api/stores/products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT products deactivate - 204 no content`() {
        mockMvc.perform(
            put("/api/stores/products/deactivate")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"productId":$productId}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT products popularity - 204 no content`() {
        mockMvc.perform(
            put("/api/stores/products/popularity")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"productId":$productId,"delta":2}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT products popularity - 400 when product not found`() {
        given(productService.incrementPopularity(storeId, productId, 2L, ownerId))
            .willThrow(NotFoundException("Product not found"))

        mockMvc.perform(
            put("/api/stores/products/popularity")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId,"productId":$productId,"delta":2}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Product not found"))
    }
}

@WebMvcTest(StoreStatisticsController::class)
@Import(SecurityConfig::class)
class StoreStatisticsControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var productStatisticsService: ProductStatisticsService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val ownerId  = 1L
    private val storeId  = 10L
    private val productId = 100L
    private val ownerPrincipal = UserPrincipal(ownerId, UserRole.OWNER)

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

    private val sampleInfo = ProductInfo(
        id = productId, storeId = storeId, name = "Burger", description = "Tasty burger",
        price = 8000L, productPictureUrl = null, popularity = 10L, status = true,
        createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun `POST popular-products - 200 with sorted list`() {
        given(productStatisticsService.getPopularProducts(storeId, ownerPrincipal))
            .willReturn(listOf(sampleInfo))

        mockMvc.perform(
            post("/api/stores/statistics/popular-products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(productId))
            .andExpect(jsonPath("$[0].popularity").value(10))
    }

    @Test
    fun `POST popular-products - 409 when wrong owner`() {
        given(productStatisticsService.getPopularProducts(storeId, ownerPrincipal))
            .willThrow(ForbiddenException("Forbidden"))

        mockMvc.perform(
            post("/api/stores/statistics/popular-products")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"storeId":$storeId}""")
        )
            .andExpect(status().isForbidden)
    }
}
