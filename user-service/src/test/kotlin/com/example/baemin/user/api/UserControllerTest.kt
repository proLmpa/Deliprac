package com.example.baemin.user.api

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.config.SecurityConfig
import com.example.baemin.user.dto.LoginCommand
import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterCommand
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.service.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
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

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val key = Keys.hmacShaKeyFor(
        "baemin-jwt-secret-key-must-be-at-least-32-characters-long".toByteArray(Charsets.UTF_8)
    )

    private fun buildToken(userId: Long, email: String, role: UserRole): String = Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("role", role.name)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + 3_600_000))
        .signWith(key)
        .compact()

    @Test
    fun `POST signup - success returns 201`() {
        val request = RegisterUserRequest("test@example.com", "password123", null)

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST signup - duplicate email returns 409 with error`() {
        val request = RegisterUserRequest("existing@example.com", "password123", null)
        given(userService.register(RegisterCommand(request.email, request.password, request.phone, request.role)))
            .willThrow(IllegalStateException("Email already exists"))

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Email already exists"))
    }

    @Test
    fun `POST signin - success returns 200 with token`() {
        val request = LoginUserRequest("test@example.com", "password123")
        given(userService.login(LoginCommand(request.email, request.password))).willReturn("jwt-token")

        mockMvc.perform(
            post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `POST signin - bad credentials returns 400 with error`() {
        val request = LoginUserRequest("test@example.com", "wrongpassword")
        given(userService.login(LoginCommand(request.email, request.password)))
            .willThrow(IllegalArgumentException("Invalid email or password"))

        mockMvc.perform(
            post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid email or password"))
    }

    @Test
    fun `POST signin - inactive account returns 409 with error`() {
        val request = LoginUserRequest("test@example.com", "password123")
        given(userService.login(LoginCommand(request.email, request.password)))
            .willThrow(IllegalStateException("Account is not active"))

        mockMvc.perform(
            post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Account is not active"))
    }

    @Test
    fun `PUT suspend - admin suspends user returns 204`() {
        val token = buildToken(99L, "admin@example.com", UserRole.ADMIN)

        mockMvc.perform(
            put("/api/users/1/suspend")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT suspend - non-admin returns 409`() {
        val token = buildToken(1L, "test@example.com", UserRole.CUSTOMER)
        val principal = UserPrincipal(1L, "test@example.com", UserRole.CUSTOMER)
        willThrow(IllegalStateException("Forbidden")).given(userService).suspend(1L, principal)

        mockMvc.perform(
            put("/api/users/1/suspend")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Forbidden"))
    }

    @Test
    fun `PUT suspend - user not found returns 400`() {
        val token = buildToken(99L, "admin@example.com", UserRole.ADMIN)
        val principal = UserPrincipal(99L, "admin@example.com", UserRole.ADMIN)
        willThrow(IllegalArgumentException("User not found")).given(userService).suspend(1L, principal)

        mockMvc.perform(
            put("/api/users/1/suspend")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("User not found"))
    }

    @Test
    fun `PUT withdraw - active user withdraws returns 204`() {
        val token = buildToken(1L, "test@example.com", UserRole.CUSTOMER)

        mockMvc.perform(
            put("/api/users/me/withdraw")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PUT withdraw - already inactive returns 409`() {
        val token = buildToken(1L, "test@example.com", UserRole.CUSTOMER)
        val principal = UserPrincipal(1L, "test@example.com", UserRole.CUSTOMER)
        willThrow(IllegalStateException("User is not active")).given(userService).withdraw(principal)

        mockMvc.perform(
            put("/api/users/me/withdraw")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("User is not active"))
    }
}
