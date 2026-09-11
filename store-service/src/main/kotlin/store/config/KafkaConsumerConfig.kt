package store.config

import common.event.OrderEvent
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory
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

private val log = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

@Configuration
class KafkaConsumerConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun orderEventDltTopic(): NewTopic =
        TopicBuilder.name("baemin.order.events.DLT").partitions(3).replicas(1).build()

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
            FixedBackOff(1000L, 2L)
        )
}
