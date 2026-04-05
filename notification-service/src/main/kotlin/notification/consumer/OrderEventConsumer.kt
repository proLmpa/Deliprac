package notification.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import notification.repository.StoreOwnerProjectionRepository
import notification.service.NotificationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

data class OrderCheckedOutEvent(val orderId: Long, val storeId: Long)
data class OrderMarkedSoldEvent(val orderId: Long, val customerId: Long)
data class OrderMarkedCanceledEvent(val orderId: Long, val customerId: Long)

@Component
class OrderEventConsumer(
    private val notificationService: NotificationService,
    private val storeOwnerProjectionRepository: StoreOwnerProjectionRepository,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(topics = ["order.checked-out"], groupId = "notification-service")
    fun onOrderCheckedOut(message: String) {
        val event = objectMapper.readValue(message, OrderCheckedOutEvent::class.java)
        val projection = storeOwnerProjectionRepository.findById(event.storeId).orElse(null) ?: return
        notificationService.create(
            userId  = projection.ownerUserId,
            title   = "New Order",
            content = "A new order #${event.orderId} has arrived."
        )
    }

    @KafkaListener(topics = ["order.marked-sold"], groupId = "notification-service")
    fun onOrderMarkedSold(message: String) {
        val event = objectMapper.readValue(message, OrderMarkedSoldEvent::class.java)
        notificationService.create(
            userId  = event.customerId,
            title   = "Order Confirmed",
            content = "Your order #${event.orderId} has been confirmed."
        )
    }

    @KafkaListener(topics = ["order.marked-canceled"], groupId = "notification-service")
    fun onOrderMarkedCanceled(message: String) {
        val event = objectMapper.readValue(message, OrderMarkedCanceledEvent::class.java)
        notificationService.create(
            userId  = event.customerId,
            title   = "Order Canceled",
            content = "Your order #${event.orderId} has been canceled."
        )
    }
}
