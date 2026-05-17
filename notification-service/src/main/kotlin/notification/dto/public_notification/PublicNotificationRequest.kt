package notification.dto.public_notification

data class CreatePublicNotificationRequest(
    val title: String,
    val content: String,
    val expiresAt: Long,
)

data class DeactivatePublicNotificationRequest(
    val notificationId: Long,
)
