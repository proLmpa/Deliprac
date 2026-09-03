package store.service.product

import common.event.OrderEvent
import common.event.OrderEventType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer (private val productService: ProductService) {

    @KafkaListener(topics = ["baemin.order.events"], groupId = "store-service")
    fun consume(event: OrderEvent) {
        if (event.eventType != OrderEventType.ORDER_SOLD) return
        event.items.forEach { item ->
            productService.incrementPopularityInternal(event.storeId, item.productId, item.quantity)
        }
    }
}