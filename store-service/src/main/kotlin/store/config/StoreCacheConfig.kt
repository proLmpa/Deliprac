package store.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "store.cache")
data class StoreCacheConfig (
    val redisTtlMinutes: Long,
    val listTtlMinutes: Long,
)