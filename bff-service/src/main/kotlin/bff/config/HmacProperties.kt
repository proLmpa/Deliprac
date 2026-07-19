package bff.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "bff.hmac")
data class HmacProperties(
    val userSecret: String,
    val storeSecret: String,
    val orderSecret: String,
    val notificationSecret: String,
    val aiSecret: String
)