package com.example.baemin.user.security

import com.example.baemin.common.security.JwtAuthenticationFilter
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Date

class JwtAuthenticationFilterTest {

    private val secret = "baemin-jwt-secret-key-must-be-at-least-32-characters-long"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    private val jwtParser = Jwts.parser().verifyWith(key).build()
    private val filter = JwtAuthenticationFilter(jwtParser)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid JWT sets UserPrincipal in SecurityContext`() {
        val userId = 1L
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", "test@example.com")
            .claim("role", UserRole.CUSTOMER.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key)
            .compact()

        val req = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $token") }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, FilterChain { _, _ -> })

        val auth = SecurityContextHolder.getContext().authentication!!
        assertNotNull(auth)
        val principal = auth.principal as UserPrincipal
        assertEquals(userId, principal.id)
        assertEquals("test@example.com", principal.email)
        assertEquals(UserRole.CUSTOMER, principal.role)
    }

    @Test
    fun `missing Authorization header leaves SecurityContext unauthenticated`() {
        val req = MockHttpServletRequest()
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, FilterChain { _, _ -> })

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `non-Bearer Authorization header leaves SecurityContext unauthenticated`() {
        val req = MockHttpServletRequest().apply { addHeader("Authorization", "Basic dXNlcjpwYXNz") }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, FilterChain { _, _ -> })

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
