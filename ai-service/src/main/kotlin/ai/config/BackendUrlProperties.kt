package ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "backend")
data class BackendUrlProperties(
    val storeUrl: String,
    val orderUrl: String
)
