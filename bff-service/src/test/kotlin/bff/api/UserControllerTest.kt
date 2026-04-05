package bff.api

import bff.client.UserClient
import bff.config.SecurityConfig
import bff.dto.LoginUserRequest
import bff.dto.RegisterUserRequest
import bff.dto.SuspendUserRequest
import bff.dto.TokenResponse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
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

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var userClient: UserClient
    @Autowired private lateinit var objectMapper: ObjectMapper

    private val token = "Bearer test-token"

    @Test
    fun `POST signup - 201`() {
        val request = RegisterUserRequest("a@b.com", "pw", null)

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST signin - 200 with token`() {
        val request = LoginUserRequest("a@b.com", "pw")
        given(userClient.signin(request)).willReturn(TokenResponse("jwt-token", "Bearer"))

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
    fun `PUT suspend - 200 and forwards token`() {
        val request = SuspendUserRequest(42L)

        mockMvc.perform(
            put("/api/users/suspend")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        verify(userClient).suspend(request, token)
    }

    @Test
    fun `PUT withdraw - 200 and forwards token`() {
        mockMvc.perform(
            put("/api/users/me/withdraw")
                .header("Authorization", token)
        )
            .andExpect(status().isOk)

        verify(userClient).withdraw(token)
    }
}
