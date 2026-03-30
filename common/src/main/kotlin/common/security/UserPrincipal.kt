package common.security

import org.springframework.security.core.context.SecurityContextHolder

data class UserPrincipal(
    val id: Long,
    val role: UserRole
)

fun currentUser(): UserPrincipal =
    SecurityContextHolder.getContext().authentication!!.principal as UserPrincipal

fun optionalCurrentUser(): UserPrincipal? {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth != null && auth.principal is UserPrincipal) auth.principal as UserPrincipal
    else null
}
