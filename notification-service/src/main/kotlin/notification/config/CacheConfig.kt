package notification.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig(
    @Value("\${notification.cache.caffeine-max-size}") private val maxSize: Long,
    @Value("\${notification.cache.caffeine-ttl-minutes}") private val ttlMinutes: Long,
) {

    @Bean
    fun caffeineCache(): Cache<String, Any> = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
        .build()
}
