package order.config

import common.event.OrderEvent
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConfig(
    private val kafkaProperties: KafkaProperties,
    private val props: KafkaTopicProperties,
) {

    @Bean
    fun orderEventTopic(): NewTopic =
        TopicBuilder.name(props.topics.orderEvents.name)
            .partitions(props.topics.orderEvents.partitions)
            .replicas(props.topics.orderEvents.replicas)
            .build()

    @Bean
    fun orderEventProducerFactory(): ProducerFactory<String, OrderEvent> =
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

    @Bean
    fun orderEventKafkaTemplate(pf: ProducerFactory<String, OrderEvent>): KafkaTemplate<String, OrderEvent> =
        KafkaTemplate(pf)
}