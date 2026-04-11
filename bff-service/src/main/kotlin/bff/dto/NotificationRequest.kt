package bff.dto

data class MarkReadRequest(val notificationId: Long)

data class ListNotificationRequest(val unreadOnly: Boolean = false)
