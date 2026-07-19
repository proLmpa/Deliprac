package ai.config

import common.security.HmacSigningInterceptor
import common.logging.MdcFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val hmacProperties: HmacProperties,
    private val backendUrlProperties: BackendUrlProperties
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
    fun storeRestClient(): RestClient =
        RestClient.builder()
            .baseUrl(backendUrlProperties.storeUrl)
            .requestFactory(factory(3, 30))
            .requestInterceptor(HmacSigningInterceptor(hmacProperties.storeSecret))
            .requestInterceptor(TraceIdInterceptor())
            .build()

    @Bean
    fun orderRestClient(): RestClient =
        RestClient.builder()
            .baseUrl(backendUrlProperties.orderUrl)
            .requestFactory(factory(3, 30))
            .requestInterceptor(HmacSigningInterceptor(hmacProperties.orderSecret))
            .requestInterceptor(TraceIdInterceptor())
            .build()
}
