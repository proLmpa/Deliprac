package notification.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

private val log = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

@Configuration
class KafkaConsumerConfig {

    /**
     * Spring Boot auto-associates a DefaultErrorHandler bean with the auto-configured
     * @KafkaListener container factory. Without this, a record that keeps failing (a
     * malformed payload, a transient downstream error) retries with the framework default
     * (FixedBackOff(0, 9) -- 10 immediate attempts) before being logged and skipped.
     * Wrapping the value deserializer in ErrorHandlingDeserializer (see application.yml)
     * ensures a deserialization failure surfaces here too, instead of throwing out of
     * Consumer.poll() and blocking the partition indefinitely.
     */
    @Bean
    fun kafkaErrorHandler(): DefaultErrorHandler =
        DefaultErrorHandler(
            { record, ex ->
                log.error(
                    "Giving up on Kafka record after retries: topic={} partition={} offset={}: {}",
                    record.topic(), record.partition(), record.offset(), ex.message, ex
                )
            },
            FixedBackOff(1000L, 2L)
        )
}
