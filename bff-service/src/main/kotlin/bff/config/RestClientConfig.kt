package bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun userClient(@Value("\${backend.user-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()

    @Bean
    fun storeClient(@Value("\${backend.store-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()

    @Bean
    fun orderClient(@Value("\${backend.order-service.url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()
}
