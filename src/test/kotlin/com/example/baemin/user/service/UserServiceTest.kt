package com.example.baemin.user.service

import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.entity.User
import com.example.baemin.user.entity.UserRole
import com.example.baemin.user.entity.UserStatus
import com.example.baemin.user.repository.UserRepository
import com.example.baemin.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `register - happy path returns saved user UUID`() {
        val request = RegisterUserRequest("test@example.com", "password123", null)
        val savedId = UUID.randomUUID()
        val savedUser = User(id = savedId, email = request.email, passwordHash = "hashed")

        given(userRepository.existsByEmail(request.email)).willReturn(false)
        given(passwordEncoder.encode(request.password)).willReturn("hashed")
        given(userRepository.save(any(User::class.java))).willReturn(savedUser)

        val result = userService.register(request)

        assertThat(result).isEqualTo(savedId)
    }

    @Test
    fun `register - duplicate email throws IllegalStateException`() {
        val request = RegisterUserRequest("existing@example.com", "password123", null)
        given(userRepository.existsByEmail(request.email)).willReturn(true)

        assertThatThrownBy { userService.register(request) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Email already exists")
    }

    @Test
    fun `register - password is hashed via passwordEncoder`() {
        val request = RegisterUserRequest("test@example.com", "rawPassword", null)
        val savedUser = User(id = UUID.randomUUID(), email = request.email, passwordHash = "hashed")

        given(userRepository.existsByEmail(request.email)).willReturn(false)
        given(passwordEncoder.encode(request.password)).willReturn("hashed")
        given(userRepository.save(any(User::class.java))).willReturn(savedUser)

        userService.register(request)

        then(passwordEncoder).should().encode("rawPassword")
    }

    @Test
    fun `login - valid credentials returns TokenResponse with accessToken and Bearer`() {
        val request = LoginUserRequest("test@example.com", "password123")
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = request.email, passwordHash = "hashed", status = UserStatus.ACTIVE)

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(true)
        given(jwtProvider.generateToken(userId, request.email, UserRole.CUSTOMER.name)).willReturn("jwt-token")

        val result = userService.login(request)

        assertThat(result.accessToken).isEqualTo("jwt-token")
        assertThat(result.tokenType).isEqualTo("Bearer")
    }

    @Test
    fun `login - email not found throws IllegalArgumentException`() {
        val request = LoginUserRequest("notfound@example.com", "password123")
        given(userRepository.findByEmail(request.email)).willReturn(null)

        assertThatThrownBy { userService.login(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `login - wrong password throws IllegalArgumentException`() {
        val request = LoginUserRequest("test@example.com", "wrongpassword")
        val user = User(id = UUID.randomUUID(), email = request.email, passwordHash = "hashed")

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(false)

        assertThatThrownBy { userService.login(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `login - suspended account throws IllegalStateException`() {
        val request = LoginUserRequest("test@example.com", "password123")
        val user = User(id = UUID.randomUUID(), email = request.email, passwordHash = "hashed", status = UserStatus.SUSPENDED)

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(true)

        assertThatThrownBy { userService.login(request) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Account is not active")
    }

    @Test
    fun `login - withdrawn account throws IllegalStateException`() {
        val request = LoginUserRequest("test@example.com", "password123")
        val user = User(id = UUID.randomUUID(), email = request.email, passwordHash = "hashed", status = UserStatus.WITHDRAWN)

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(true)

        assertThatThrownBy { userService.login(request) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Account is not active")
    }
}
