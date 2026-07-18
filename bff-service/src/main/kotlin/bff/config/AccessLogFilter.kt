package bff.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

class AccessLogFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = System.currentTimeMillis() - start
            log.info {
                "method=${request.method} uri=${request.requestURI} " +
                "status=${response.status} durationMs=$durationMs"
            }
        }
    }
}

@Configuration
class AccessLogConfig {
    @Bean
    fun accessLogFilterRegistration(): FilterRegistrationBean<AccessLogFilter> =
        FilterRegistrationBean(AccessLogFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 1
            addUrlPatterns("/*")
        }
}