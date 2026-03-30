package user.service

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.orThrow
import common.security.UserRole
import user.dto.LoginCommand
import user.dto.RegisterCommand
import user.entity.User
import user.entity.UserStatus
import user.repository.UserRepository
import user.security.JwtProvider
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    val userRepository: UserRepository,
    val passwordEncoder: PasswordEncoder,
    val jwtProvider: JwtProvider
) {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    @Transactional
    fun register(command: RegisterCommand): Long {
        if (!emailRegex.matches(command.email)) throw IllegalArgumentException("Invalid email format")

        if (userRepository.existsByEmail(command.email)) {
            throw ConflictException("Email already exists")
        }

        val role = runCatching { UserRole.valueOf(command.role) }
            .getOrElse { throw IllegalArgumentException("Invalid role: ${command.role}") }

        val user = User(
            id = 0L,
            email = command.email,
            passwordHash = passwordEncoder.encode(command.password),
            phone = command.phone,
            role = role,
        )

        return userRepository.save(user).id
    }

    fun login(command: LoginCommand): String {
        val user = userRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(command.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw ConflictException("Account is not active")
        }

        return jwtProvider.generateToken(user.id, user.email, user.role.name)
    }

    @Transactional
    fun suspend(targetUserId: Long, role: UserRole) {
        if (role != UserRole.ADMIN) throw ForbiddenException("Forbidden")

        val user = userRepository.findById(targetUserId).orThrow("User not found")
        if (user.status != UserStatus.ACTIVE) throw ConflictException("User is not active")

        user.status = UserStatus.SUSPENDED
    }

    @Transactional
    fun withdraw(userId: Long) {
        val user = userRepository.findById(userId).orThrow("User not found")
        if (user.status != UserStatus.ACTIVE) throw ConflictException("User is not active")

        user.status = UserStatus.WITHDRAWN
    }
}
