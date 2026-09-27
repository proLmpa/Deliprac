package order.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka")
data class KafkaTopicProperties(val topics: Topics) {
    data class Topics(val orderEvents: TopicConfig)
    data class TopicConfig(val name: String, val partitions: Int, val replicas: Int)
}
