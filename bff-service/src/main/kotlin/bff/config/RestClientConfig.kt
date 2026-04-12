package bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {

    private fun factory(connectSecs: Long, readSecs: Long): JdkClientHttpRequestFactory {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectSecs))
            .build()
        return JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(Duration.ofSeconds(readSecs))
        }
    }

    @Bean
    fun userRestClient(@Value("\${backend.user-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(factory(3, 5)).build()

    @Bean
    fun storeRestClient(@Value("\${backend.store-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(factory(3, 5)).build()

    @Bean
    fun orderRestClient(@Value("\${backend.order-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(factory(3, 5)).build()

    @Bean
    fun notificationRestClient(@Value("\${backend.notification-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(factory(3, 3)).build()
}
