package com.example.baemin.user.security

import com.example.baemin.user.entity.UserRole
import io.jsonwebtoken.JwtParser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class JwtAuthenticationFilter(private val jwtParser: JwtParser) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val token = req.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")

        if (token != null) {
            val claims = jwtParser.parseSignedClaims(token).payload
            val principal = UserPrincipal(
                id    = UUID.fromString(claims.subject),
                email = claims["email"] as String,
                role  = UserRole.valueOf(claims["role"] as String)
            )
            val auth = UsernamePasswordAuthenticationToken(
                principal, null, listOf(SimpleGrantedAuthority(principal.role.name))
            )
            SecurityContextHolder.getContext().authentication = auth
        }

        chain.doFilter(req, res)
    }
}
