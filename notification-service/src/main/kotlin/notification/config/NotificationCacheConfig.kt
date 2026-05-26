package notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.cache")
data class NotificationCacheConfig (
    val caffeineMaxSize: Long,
    val caffeineTtlMinutes: Long,
    val key: String,
    val redisTtlMinutes: Long
)