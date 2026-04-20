package bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig(
    @Value("\${bff.hmac.user-service.secret}")           private val userSecret: String,
    @Value("\${bff.hmac.store-service.secret}")          private val storeSecret: String,
    @Value("\${bff.hmac.order-service.secret}")          private val orderSecret: String,
    @Value("\${bff.hmac.notification-service.secret}")   private val notifSecret: String,
) {

    private fun factory(connectSecs: Long, readSecs: Long): JdkClientHttpRequestFactory {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectSecs))
            .build()
        return JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(Duration.ofSeconds(readSecs))
        }
    }

    @Bean
    fun userRestClient(@Value("\${backend.user-service.url}") url: String): RestClient =
        RestClient.builder().baseUrl(url).requestFactory(factory(3, 5))
            .requestInterceptor(HmacSigningInterceptor(userSecret)).build()

    @Bean
    fun storeRestClient(@Value("\${backend.store-service.url}") url: String): RestClient =
        RestClient.builder().baseUrl(url).requestFactory(factory(3, 5))
            .requestInterceptor(HmacSigningInterceptor(storeSecret)).build()

    @Bean
    fun orderRestClient(@Value("\${backend.order-service.url}") url: String): RestClient =
        RestClient.builder().baseUrl(url).requestFactory(factory(3, 5))
            .requestInterceptor(HmacSigningInterceptor(orderSecret)).build()

    @Bean
    fun notificationRestClient(@Value("\${backend.notification-service.url}") url: String): RestClient =
        RestClient.builder().baseUrl(url).requestFactory(factory(3, 3))
            .requestInterceptor(HmacSigningInterceptor(notifSecret)).build()
}
