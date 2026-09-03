package order.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {
    @Bean
    fun orderEventTopic(): NewTopic =
        TopicBuilder.name("baemin.order.events").partitions(3).replicas(1).build()
}
