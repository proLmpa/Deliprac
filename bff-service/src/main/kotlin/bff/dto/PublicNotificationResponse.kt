package bff.dto

data class PublicNotificationResponse(
    val id: Long,
    val title: String,
    val content: String,
    val isActive: Boolean,
    val issuedAt: Long,
    val expiresAt: Long,
)
