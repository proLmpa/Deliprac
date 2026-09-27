package notification.config

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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties,
    private val props: KafkaTopicProperties,
) {

    @Bean
    fun orderEventDltTopic(): NewTopic =
        TopicBuilder.name(props.topics.orderEventsDlt.name)
            .partitions(props.topics.orderEventsDlt.partitions)
            .replicas(props.topics.orderEventsDlt.replicas)
            .build()

    @Bean
    fun dltProducerFactory(): ProducerFactory<String, OrderEvent> =
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

    @Bean
    fun dltKafkaTemplate(pf: ProducerFactory<String, OrderEvent>): KafkaTemplate<String, OrderEvent> =
        KafkaTemplate(pf)

    @Bean
    fun kafkaErrorHandler(dltKafkaTemplate: KafkaTemplate<String, OrderEvent>): DefaultErrorHandler =
        DefaultErrorHandler(
            DeadLetterPublishingRecoverer(dltKafkaTemplate),
            FixedBackOff(props.consumer.backoffIntervalMs, props.consumer.maxAttempts)
        )
}
