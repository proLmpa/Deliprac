package com.example.baemin.user.security

import com.example.baemin.user.entity.UserRole
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
    val role: UserRole
)

fun currentUser(): UserPrincipal =
    SecurityContextHolder.getContext().authentication!!.principal as UserPrincipal
