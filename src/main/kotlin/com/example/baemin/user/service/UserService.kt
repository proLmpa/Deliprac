package com.example.baemin.user.service

import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.dto.TokenResponse
import com.example.baemin.user.entity.User
import com.example.baemin.user.entity.UserRole
import com.example.baemin.user.entity.UserStatus
import com.example.baemin.user.repository.UserRepository
import com.example.baemin.user.security.JwtProvider
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    val userRepository: UserRepository,
    val passwordEncoder: PasswordEncoder,
    val jwtProvider: JwtProvider
) {

    @Transactional
    fun register(request: RegisterUserRequest): UUID {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalStateException("Email already exists")
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            phone = request.phone,
            role = UserRole.CUSTOMER
        )

        return userRepository.save(user).id!!
    }

    fun login(request: LoginUserRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalStateException("Account is not active")
        }

        val token = jwtProvider.generateToken(user.id!!, user.email!!, user.role.name)
        return TokenResponse(accessToken = token)
    }
}
