package order.config

import common.logging.MdcFilter
import common.security.JwtAuthenticationFilter
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProperties: JwtProperties
) {

    @Bean
    fun jwtParser(): JwtParser = Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8)))
        .build()

    @Bean
    fun jwtAuthFilter(jwtParser: JwtParser): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtParser)

    @Bean
    fun jwtAuthFilterRegistration(jwtAuthFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> =
        FilterRegistrationBean(jwtAuthFilter).apply { isEnabled = false }

    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun mdcFilterRegistration(): FilterRegistrationBean<MdcFilter> =
        FilterRegistrationBean(MdcFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
