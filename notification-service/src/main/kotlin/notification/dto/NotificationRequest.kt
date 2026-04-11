package notification.dto

import notification.entity.NotificationType

data class NotificationItemRequest(
    val productName: String,
    val unitPrice: Long,
    val quantity: Long,
)

data class CreateNotificationRequest(
    val recipientId: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val storeId: Long? = null,
    val storeName: String? = null,
    val expiry: Long,
    val items: List<NotificationItemRequest> = emptyList(),
)

data class MarkReadRequest(val notificationId: Long)

data class ListNotificationRequest(val unreadOnly: Boolean = false)
