package ai.config

import common.logging.MdcFilter
import common.security.HmacRequestFilter
import common.security.JwtAuthenticationFilter
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    private val hmacProperties: HmacProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val key = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        val jwtParser = Jwts.parser().verifyWith(key).build()

        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(HmacRequestFilter(hmacProperties.aiSecret), UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(JwtAuthenticationFilter(jwtParser), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/**").permitAll()
                it.anyRequest().authenticated()
            }
        return http.build()
    }

    @Bean
    fun jwtAuthFilterRegistration(
        @Value("\${jwt.secret}") secret: String
    ): FilterRegistrationBean<JwtAuthenticationFilter> {
        val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val jwtParser = Jwts.parser().verifyWith(key).build()
        return FilterRegistrationBean(JwtAuthenticationFilter(jwtParser)).apply { isEnabled = false }
    }

    @Bean
    fun mdcFilterRegistration(): FilterRegistrationBean<MdcFilter> =
        FilterRegistrationBean(MdcFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
