package order.config

import common.event.OrderEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger {}

/**
 * Publishes [OrderEvent]s to Kafka only after the enclosing DB transaction commits.
 *
 * CartService/OrderService publish via ApplicationEventPublisher.publishEvent(event) from
 * inside a @Transactional method instead of calling KafkaTemplate directly. Sending to Kafka
 * synchronously inside the transaction risks publishing an event for an order that is then
 * rolled back; AFTER_COMMIT guarantees the DB write has succeeded first.
 */
@Component
class OrderEventRelay(
    private val kafkaTemplate: KafkaTemplate<String, OrderEvent>,
    @Value("\${kafka.topics.order-events.name}") private val orderEventTopicName: String,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderEvent(event: OrderEvent) {
        kafkaTemplate.send(orderEventTopicName, event.storeId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error(ex) { "Failed to publish ${event.eventType} for order ${event.orderId}: ${ex.message}" }
                } else {
                    log.debug {
                        "Published ${event.eventType} for order ${event.orderId} " +
                        "(partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()})"
                    }
                }
            }
    }
}
