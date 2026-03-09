package com.example.baemin.user.service

import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.user.dto.LoginCommand
import com.example.baemin.user.dto.RegisterCommand
import com.example.baemin.user.entity.User
import com.example.baemin.user.entity.UserStatus
import com.example.baemin.user.repository.UserRepository
import com.example.baemin.user.security.JwtProvider
import jakarta.transaction.Transactional
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
    fun register(command: RegisterCommand) {
        if (!emailRegex.matches(command.email)) throw IllegalArgumentException("Invalid email format")

        if (userRepository.existsByEmail(command.email)) {
            throw IllegalStateException("Email already exists")
        }

        val role = runCatching { UserRole.valueOf(command.role) }
            .getOrElse { throw IllegalArgumentException("Invalid role: ${command.role}") }

        val now = System.currentTimeMillis()
        val user = User(
            id = 0L,
            email = command.email,
            passwordHash = passwordEncoder.encode(command.password),
            phone = command.phone ?: "",
            role = role,
            createdAt = now,
            updatedAt = now
        )

        userRepository.save(user)
    }

    fun login(command: LoginCommand): String {
        val user = userRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(command.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalStateException("Account is not active")
        }

        return jwtProvider.generateToken(user.id, user.email, user.role.name)
    }

    @Transactional
    fun suspend(targetUserId: Long, principal: UserPrincipal) {
        if (principal.role != UserRole.ADMIN) throw IllegalStateException("Forbidden")

        val user = userRepository.findById(targetUserId).orThrow("User not found")
        if (user.status != UserStatus.ACTIVE) throw IllegalStateException("User is not active")

        user.status = UserStatus.SUSPENDED
        user.updatedAt = System.currentTimeMillis()
    }

    @Transactional
    fun withdraw(principal: UserPrincipal) {
        val user = userRepository.findById(principal.id).orThrow("User not found")
        if (user.status != UserStatus.ACTIVE) throw IllegalStateException("User is not active")

        user.status = UserStatus.WITHDRAWN
        user.updatedAt = System.currentTimeMillis()
    }
}
