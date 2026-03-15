package common.security

import org.springframework.security.core.context.SecurityContextHolder

data class UserPrincipal(
    val id: Long,
    val role: UserRole
)

fun currentUser(): UserPrincipal =
    SecurityContextHolder.getContext().authentication!!.principal as UserPrincipal
