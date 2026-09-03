package notification.service.user

import common.event.OrderEvent
import common.event.OrderEventType
import notification.entity.user.NotificationType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private data class NotifContext (
    val recipientId: Long,
    val type: NotificationType,
    val title: String,
    val content: String
)

@Component
class OrderEventConsumer (private val notificationService: NotificationService) {

    @KafkaListener(topics = ["baemin.order.events"], groupId = "notification-service")
    fun consume(event: OrderEvent) {
        val ctx = when (event.eventType) {
            OrderEventType.NEW_ORDER -> NotifContext(
                recipientId = event.storeOwnerId,
                type = NotificationType.NEW_ORDER,
                title = "새 주문 접수",
                content = "새 주문이 접수되었습니다."
            )
            OrderEventType.ORDER_SOLD -> NotifContext(
                recipientId = event.userId,
                type = NotificationType.ORDER_SOLD,
                title = "주문 완료",
                content = "주문이 완료되었습니다."
            )
            OrderEventType.ORDER_CANCELLED -> NotifContext(
                recipientId = event.userId,
                type = NotificationType.ORDER_CANCELED,
                title = "주문 취소",
                content = "주문이 취소되었습니다."
            )
        }

        notificationService.createFromEvent(
            recipientId = ctx.recipientId,
            type = ctx.type,
            title = ctx.title,
            content = ctx.content,
            storeId = event.storeId,
            storeName = event.storeName.ifBlank { null },
            items = event.items,
            occurredAt = event.occurredAt
        )
    }
}