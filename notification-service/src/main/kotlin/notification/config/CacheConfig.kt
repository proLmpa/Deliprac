package notification.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig(
    private val notifCacheConfig: NotificationCacheConfig
) {

    @Bean
    fun caffeineCache(): Cache<String, Any> = Caffeine.newBuilder()
        .maximumSize(notifCacheConfig.caffeineMaxSize)
        .expireAfterWrite(notifCacheConfig.caffeineTtlMinutes, TimeUnit.MINUTES)
        .build()
}
