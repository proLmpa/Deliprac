package ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "bff.hmac")
data class HmacProperties(
    val aiSecret: String,
    val storeSecret: String,
    val orderSecret: String
)
