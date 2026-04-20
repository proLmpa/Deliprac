package store.config

import common.security.HmacRequestFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class HmacConfig(
    @Value("\${bff.hmac.secret}") private val secret: String,
    @Value("\${bff.hmac.window-ms:30000}") private val windowMs: Long
) {
    @Bean
    fun hmacRequestFilter() = HmacRequestFilter(secret, windowMs)

    @Bean
    fun hmacFilterRegistration(f: HmacRequestFilter): FilterRegistrationBean<HmacRequestFilter> =
        FilterRegistrationBean(f).apply {
            urlPatterns = setOf("/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
