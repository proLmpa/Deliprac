package user.service

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.security.UserRole
import user.dto.LoginCommand
import user.dto.RegisterCommand
import user.entity.User
import user.entity.UserStatus
import user.repository.UserRepository
import user.security.JwtProvider
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
import java.util.Optional

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
    fun `register - happy path saves user`() {
        val command = RegisterCommand("test@example.com", "password123", null)
        val savedUser = User(id = 1L, email = command.email, phone = "", passwordHash = "hashed", createdAt = 0L, updatedAt = 0L)

        given(userRepository.existsByEmail(command.email)).willReturn(false)
        given(passwordEncoder.encode(command.password)).willReturn("hashed")
        given(userRepository.save(any(User::class.java))).willReturn(savedUser)

        userService.register(command)

        then(userRepository).should().save(any(User::class.java))
    }

    @Test
    fun `register - duplicate email throws ConflictException`() {
        val command = RegisterCommand("existing@example.com", "password123", null)
        given(userRepository.existsByEmail(command.email)).willReturn(true)

        assertThatThrownBy { userService.register(command) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("Email already exists")
    }

    @Test
    fun `register - password is hashed via passwordEncoder`() {
        val command = RegisterCommand("test@example.com", "rawPassword", null)
        val savedUser = User(id = 1L, email = command.email, phone = "", passwordHash = "hashed", createdAt = 0L, updatedAt = 0L)

        given(userRepository.existsByEmail(command.email)).willReturn(false)
        given(passwordEncoder.encode(command.password)).willReturn("hashed")
        given(userRepository.save(any(User::class.java))).willReturn(savedUser)

        userService.register(command)

        then(passwordEncoder).should().encode("rawPassword")
    }

    @Test
    fun `login - valid credentials returns jwt token string`() {
        val command = LoginCommand("test@example.com", "password123")
        val userId = 1L
        val user = User(id = userId, email = command.email, phone = "", passwordHash = "hashed", status = UserStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

        given(userRepository.findByEmail(command.email)).willReturn(user)
        given(passwordEncoder.matches(command.password, user.passwordHash)).willReturn(true)
        given(jwtProvider.generateToken(userId, command.email, UserRole.CUSTOMER.name)).willReturn("jwt-token")

        val result = userService.login(command)

        assertThat(result).isEqualTo("jwt-token")
    }

    @Test
    fun `login - email not found throws IllegalArgumentException`() {
        val command = LoginCommand("notfound@example.com", "password123")
        given(userRepository.findByEmail(command.email)).willReturn(null)

        assertThatThrownBy { userService.login(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `login - wrong password throws IllegalArgumentException`() {
        val command = LoginCommand("test@example.com", "wrongpassword")
        val user = User(id = 1L, email = command.email, phone = "", passwordHash = "hashed", createdAt = 0L, updatedAt = 0L)

        given(userRepository.findByEmail(command.email)).willReturn(user)
        given(passwordEncoder.matches(command.password, user.passwordHash)).willReturn(false)

        assertThatThrownBy { userService.login(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `login - suspended account throws ConflictException`() {
        val command = LoginCommand("test@example.com", "password123")
        val user = User(id = 1L, email = command.email, phone = "", passwordHash = "hashed", status = UserStatus.SUSPENDED, createdAt = 0L, updatedAt = 0L)

        given(userRepository.findByEmail(command.email)).willReturn(user)
        given(passwordEncoder.matches(command.password, user.passwordHash)).willReturn(true)

        assertThatThrownBy { userService.login(command) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("Account is not active")
    }

    @Test
    fun `login - withdrawn account throws ConflictException`() {
        val command = LoginCommand("test@example.com", "password123")
        val user = User(id = 1L, email = command.email, phone = "", passwordHash = "hashed", status = UserStatus.WITHDRAWN, createdAt = 0L, updatedAt = 0L)

        given(userRepository.findByEmail(command.email)).willReturn(user)
        given(passwordEncoder.matches(command.password, user.passwordHash)).willReturn(true)

        assertThatThrownBy { userService.login(command) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("Account is not active")
    }

    @Test
    fun `suspend - admin suspends active user`() {
        val user = User(id = 1L, email = "test@example.com", phone = "", passwordHash = "hashed", status = UserStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        userService.suspend(1L, UserRole.ADMIN)

        assertThat(user.status).isEqualTo(UserStatus.SUSPENDED)
    }

    @Test
    fun `suspend - non-admin throws ForbiddenException`() {
        assertThatThrownBy { userService.suspend(1L, UserRole.CUSTOMER) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }

    @Test
    fun `suspend - user not found throws NotFoundException`() {
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy { userService.suspend(1L, UserRole.ADMIN) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("User not found")
    }

    @Test
    fun `suspend - already inactive throws ConflictException`() {
        val user = User(id = 1L, email = "test@example.com", phone = "", passwordHash = "hashed", status = UserStatus.SUSPENDED, createdAt = 0L, updatedAt = 0L)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        assertThatThrownBy { userService.suspend(1L, UserRole.ADMIN) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("User is not active")
    }

    @Test
    fun `withdraw - active user withdraws`() {
        val user = User(id = 1L, email = "test@example.com", phone = "", passwordHash = "hashed", status = UserStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        userService.withdraw(1L)

        assertThat(user.status).isEqualTo(UserStatus.WITHDRAWN)
    }

    @Test
    fun `withdraw - already inactive throws ConflictException`() {
        val user = User(id = 1L, email = "test@example.com", phone = "", passwordHash = "hashed", status = UserStatus.WITHDRAWN, createdAt = 0L, updatedAt = 0L)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        assertThatThrownBy { userService.withdraw(1L) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("User is not active")
    }
}
