package order.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "order.cache")
data class OrderCacheConfig (
    val redisTtlMinutes: Long
)