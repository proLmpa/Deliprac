package order.config

import com.fasterxml.jackson.databind.ObjectMapper
import common.event.OrderEvent
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String
) {
    private val mapper = ObjectMapper().apply { findAndRegisterModules() }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, OrderEvent> {
        val valueSerializer = Serializer<OrderEvent> { _, data ->
            data?.let { mapper.writeValueAsBytes(it) }
        }
        return KafkaTemplate(DefaultKafkaProducerFactory(
            mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers),
            StringSerializer(),
            valueSerializer
        ))
    }

    @Bean
    fun orderEventTopic(): NewTopic =
        TopicBuilder.name("baemin.order.events").partitions(3).replicas(1).build()
}
