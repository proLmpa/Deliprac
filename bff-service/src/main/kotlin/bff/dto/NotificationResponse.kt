package bff.dto

data class NotificationItemResponse(
    val productName: String,
    val unitPrice: Long,
    val quantity: Long,
)

data class NotificationResponse(
    val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val storeId: Long?,
    val storeName: String?,
    val isRead: Boolean,
    val issuedAt: Long,
    val expiry: Long,
    val createdAt: Long,
    val items: List<NotificationItemResponse> = emptyList(),
)
