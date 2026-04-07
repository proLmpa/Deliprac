package notification.dto

import notification.entity.Notification

data class NotificationResponse(
    val id: Long,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val issuedAt: Long,
    val expiry: Long,
    val createdAt: Long
) {
    companion object {
        fun of(n: Notification) = NotificationResponse(
            id        = n.id,
            title     = n.title,
            content   = n.content,
            isRead    = n.isRead,
            issuedAt  = n.issuedAt,
            expiry    = n.expiry,
            createdAt = n.createdAt
        )
    }
}
