package order.config

import common.event.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(OrderEventRelay::class.java)

/**
 * Publishes [OrderEvent]s to Kafka only after the enclosing DB transaction commits.
 *
 * CartService/OrderService publish via ApplicationEventPublisher.publishEvent(event) from
 * inside a @Transactional method instead of calling KafkaTemplate directly. Sending to Kafka
 * synchronously inside the transaction risks publishing an event for an order that is then
 * rolled back; AFTER_COMMIT guarantees the DB write has succeeded first.
 */
@Component
class OrderEventRelay(private val kafkaTemplate: KafkaTemplate<String, OrderEvent>) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderEvent(event: OrderEvent) {
        kafkaTemplate.send("baemin.order.events", event.storeId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("Failed to publish {} for order {}: {}", event.eventType, event.orderId, ex.message, ex)
                } else {
                    log.debug(
                        "Published {} for order {} (partition={}, offset={})",
                        event.eventType, event.orderId,
                        result.recordMetadata.partition(), result.recordMetadata.offset()
                    )
                }
            }
    }
}
