package bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun userRestClient(@Value("\${backend.user-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()

    @Bean
    fun storeRestClient(@Value("\${backend.store-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()

    @Bean
    fun orderRestClient(@Value("\${backend.order-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()

    @Bean
    fun notificationRestClient(@Value("\${backend.notification-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()
}
