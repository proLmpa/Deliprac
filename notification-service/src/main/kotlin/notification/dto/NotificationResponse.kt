package notification.dto

import notification.entity.Notification

data class NotificationResponse(
    val id: Long,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long
) {
    companion object {
        fun of(n: Notification) = NotificationResponse(
            id        = n.id,
            title     = n.title,
            content   = n.content,
            isRead    = n.isRead,
            createdAt = n.createdAt
        )
    }
}
