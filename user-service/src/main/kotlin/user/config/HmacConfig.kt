package user.config

import common.security.HmacRequestFilter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@ConfigurationProperties(prefix = "bff.hmac")
data class BffHmacProperties(
    val secret: String,
    val windowMs: Long = 30000L
)

@Configuration
class HmacConfig(private val props: BffHmacProperties) {
    @Bean
    fun hmacRequestFilter() = HmacRequestFilter(props.secret, props.windowMs)

    @Bean
    fun hmacFilterRegistration(f: HmacRequestFilter): FilterRegistrationBean<HmacRequestFilter> =
        FilterRegistrationBean(f).apply {
            urlPatterns = setOf("/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
