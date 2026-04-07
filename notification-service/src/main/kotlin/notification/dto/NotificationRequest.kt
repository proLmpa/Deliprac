package notification.dto

data class CreateNotificationRequest(
    val recipientId: Long,
    val title: String,
    val content: String,
    val expiry: Long
)

data class MarkReadRequest(val notificationId: Long)

data class ListNotificationRequest(val unreadOnly: Boolean = false)
