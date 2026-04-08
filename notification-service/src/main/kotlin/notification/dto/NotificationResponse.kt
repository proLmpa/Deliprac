package notification.dto

import notification.entity.Notification
import notification.entity.NotificationType

data class NotificationItemResponse(
    val productName: String,
    val unitPrice: Long,
    val quantity: Long,
)

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val storeId: Long?,
    val storeName: String?,
    val isRead: Boolean,
    val issuedAt: Long,
    val expiry: Long,
    val createdAt: Long,
    val items: List<NotificationItemResponse>,
) {
    companion object {
        fun of(n: Notification) = NotificationResponse(
            id        = n.id,
            type      = n.type,
            title     = n.title,
            content   = n.content,
            storeId   = n.storeId,
            storeName = n.storeName,
            isRead    = n.isRead,
            issuedAt  = n.issuedAt,
            expiry    = n.expiry,
            createdAt = n.createdAt,
            items     = n.items.map { NotificationItemResponse(it.productName, it.unitPrice, it.quantity) }
        )
    }
}
