package store.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka")
data class KafkaTopicProperties (
    val topics: Topics,
    val consumer: ConsumerConfig = ConsumerConfig()
) {
    data class Topics(val orderEvents: TopicConfig, val orderEventsDlt: TopicConfig)
    data class TopicConfig(val name: String, val partitions: Int = 3, val replicas: Int = 1)
    data class ConsumerConfig(val backoffIntervalMs: Long = 1000L, val maxAttempts: Long = 2L)
}