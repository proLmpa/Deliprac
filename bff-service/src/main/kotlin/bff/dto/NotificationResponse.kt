package bff.dto

data class NotificationResponse(
    val id: Long,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long
)
