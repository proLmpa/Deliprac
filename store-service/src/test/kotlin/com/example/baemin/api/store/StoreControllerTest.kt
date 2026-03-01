package com.example.baemin.api.store

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.config.SecurityConfig
import com.example.baemin.dto.store.CreateStoreCommand
import com.example.baemin.dto.store.CreateStoreRequest
import com.example.baemin.dto.store.StoreInfo
import com.example.baemin.dto.store.UpdateStoreCommand
import com.example.baemin.dto.store.UpdateStoreRequest
import com.example.baemin.service.store.StoreService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.Date

@WebMvcTest(StoreController::class)
@Import(SecurityConfig::class)
class StoreControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var storeService: StoreService
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Value("\${jwt.secret}") private lateinit var jwtSecret: String

    private val ownerId = 1L
    private val storeId = 2L

    private val ownerPrincipal = UserPrincipal(ownerId, "owner@example.com", UserRole.OWNER)

    private fun bearerToken(userId: Long = ownerId, email: String = "owner@example.com", role: UserRole = UserRole.OWNER): String {
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

    private val sampleInfo = StoreInfo(
        id                 = storeId,
        name               = "Test Store",
        address            = "Seoul Gangnam-gu",
        phone              = "02-1234-5678",
        content            = "Good food",
        status             = "ACTIVE",
        storePictureUrl    = null,
        productCreatedTime = 32400000L,
        openedTime         = 36000000L,
        closedTime         = 79200000L,
        closedDays         = "MONDAY",
        createdAt          = 1704067200000L,
        updatedAt          = 1704067200000L
    )

    private val createRequest = CreateStoreRequest(
        name               = "Test Store",
        address            = "Seoul Gangnam-gu",
        phone              = "02-1234-5678",
        content            = "Good food",
        storePictureUrl    = null,
        productCreatedTime = 32400000L,
        openedTime         = 36000000L,
        closedTime         = 79200000L,
        closedDays         = "MONDAY"
    )

    private val createCommand = CreateStoreCommand(
        name               = "Test Store",
        address            = "Seoul Gangnam-gu",
        phone              = "02-1234-5678",
        content            = "Good food",
        storePictureUrl    = null,
        productCreatedTime = 32400000L,
        openedTime         = 36000000L,
        closedTime         = 79200000L,
        closedDays         = "MONDAY"
    )

    private val updateRequest = UpdateStoreRequest(
        name               = "Updated Store",
        address            = "Seoul Mapo-gu",
        phone              = "02-9876-5432",
        content            = "Even better food",
        storePictureUrl    = null,
        productCreatedTime = 28800000L,
        openedTime         = 32400000L,
        closedTime         = 75600000L,
        closedDays         = "SUNDAY"
    )

    private val updateCommand = UpdateStoreCommand(
        name               = "Updated Store",
        address            = "Seoul Mapo-gu",
        phone              = "02-9876-5432",
        content            = "Even better food",
        storePictureUrl    = null,
        productCreatedTime = 28800000L,
        openedTime         = 32400000L,
        closedTime         = 75600000L,
        closedDays         = "SUNDAY"
    )

    @Test
    fun `POST stores - 201 with store response`() {
        given(storeService.create(createCommand, ownerPrincipal)).willReturn(sampleInfo)

        mockMvc.perform(
            post("/api/stores")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(storeId))
            .andExpect(jsonPath("$.name").value("Test Store"))
    }

    @Test
    fun `POST stores - 409 when service throws IllegalStateException`() {
        given(storeService.create(createCommand, ownerPrincipal))
            .willThrow(IllegalStateException("Store already exists for this owner"))

        mockMvc.perform(
            post("/api/stores")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Store already exists for this owner"))
    }

    @Test
    fun `GET stores - 200 with list`() {
        given(storeService.listAll()).willReturn(listOf(sampleInfo))

        mockMvc.perform(
            get("/api/stores")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(storeId))
    }

    @Test
    fun `GET stores by id - 200 with store response`() {
        given(storeService.findById(storeId)).willReturn(sampleInfo)

        mockMvc.perform(
            get("/api/stores/{id}", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(storeId))
            .andExpect(jsonPath("$.name").value("Test Store"))
    }

    @Test
    fun `GET stores by id - 400 when not found`() {
        given(storeService.findById(storeId))
            .willThrow(IllegalArgumentException("Store not found"))

        mockMvc.perform(
            get("/api/stores/{id}", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Store not found"))
    }

    @Test
    fun `GET stores mine - 200 with owner store`() {
        given(storeService.findMine(ownerPrincipal)).willReturn(sampleInfo)

        mockMvc.perform(
            get("/api/stores/mine")
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(storeId))
    }

    @Test
    fun `PUT stores by id - 200 with updated store`() {
        val updated = sampleInfo.copy(name = "Updated Store")
        given(storeService.update(storeId, updateCommand, ownerPrincipal)).willReturn(updated)

        mockMvc.perform(
            put("/api/stores/{id}", storeId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Store"))
    }

    @Test
    fun `PUT stores deactivate - 204 no content`() {
        mockMvc.perform(
            put("/api/stores/{id}/deactivate", storeId)
                .header("Authorization", bearerToken())
        )
            .andExpect(status().isNoContent)
    }
}
