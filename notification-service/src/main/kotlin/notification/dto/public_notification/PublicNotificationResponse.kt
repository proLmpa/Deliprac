package notification.dto.public_notification

import notification.entity.public_notification.PublicNotification

data class PublicNotificationResponse(
    val id: Long,
    val title: String,
    val content: String,
    val isActive: Boolean,
    val issuedAt: Long,
    val expiresAt: Long,
) {
    companion object {
        fun of(n: PublicNotification) = PublicNotificationResponse(
            id       = n.id,
            title    = n.title,
            content  = n.content,
            isActive = n.isActive,
            issuedAt = n.issuedAt,
            expiresAt = n.expiresAt,
        )
    }
}
