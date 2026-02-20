package com.example.baemin.user.api

import com.example.baemin.config.SecurityConfig
import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.dto.TokenResponse
import com.example.baemin.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST signup - success returns 201 with user id`() {
        val request = RegisterUserRequest("test@example.com", "password123", null)
        val userId = UUID.randomUUID()
        given(userService.register(request)).willReturn(userId)

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(userId.toString()))
    }

    @Test
    fun `POST signup - duplicate email returns 409 with error`() {
        val request = RegisterUserRequest("existing@example.com", "password123", null)
        given(userService.register(request)).willThrow(IllegalStateException("Email already exists"))

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
        val tokenResponse = TokenResponse(accessToken = "jwt-token")
        given(userService.login(request)).willReturn(tokenResponse)

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
        given(userService.login(request)).willThrow(IllegalArgumentException("Invalid email or password"))

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
        given(userService.login(request)).willThrow(IllegalStateException("Account is not active"))

        mockMvc.perform(
            post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Account is not active"))
    }
}
