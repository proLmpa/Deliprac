package bff.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "backend")
data class BackendUrlProperties(
    val userUrl: String,
    val storeUrl: String,
    val orderUrl: String,
    val notificationUrl: String,
    val aiUrl: String
)