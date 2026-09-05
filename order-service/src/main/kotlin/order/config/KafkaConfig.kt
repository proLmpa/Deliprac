package order.config

import common.event.OrderEvent
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun orderEventTopic(): NewTopic =
        TopicBuilder.name("baemin.order.events").partitions(3).replicas(1).build()

    @Bean
    fun orderEventProducerFactory(): ProducerFactory<String, OrderEvent> =
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

    @Bean
    fun orderEventKafkaTemplate(pf: ProducerFactory<String, OrderEvent>): KafkaTemplate<String, OrderEvent> =
        KafkaTemplate(pf)
}
