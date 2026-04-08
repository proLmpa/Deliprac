package bff.client

import bff.dto.ListNotificationRequest
import bff.dto.MarkReadRequest
import bff.dto.NotificationResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

// Internal DTOs: match notification-service's CreateNotificationRequest
data class NotificationItemData(val productName: String, val unitPrice: Long, val quantity: Long)

data class CreateNotificationRequest(
    val recipientId: Long,
    val type: String,           // "NEW_ORDER" | "ORDER_SOLD" | "ORDER_CANCELED"
    val title: String,
    val content: String,
    val storeId: Long? = null,
    val storeName: String? = null,
    val expiry: Long,
    val items: List<NotificationItemData> = emptyList(),
)

@Component
class NotificationClient(@Qualifier("notificationRestClient") private val client: RestClient) {

    fun createNotification(request: CreateNotificationRequest) {
        client.post()
            .uri("/internal/notifications")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    fun listMyNotifications(request: ListNotificationRequest, token: String): List<NotificationResponse> =
        client.post()
            .uri("/api/notifications/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<NotificationResponse>>() {})!!

    fun markRead(request: MarkReadRequest, token: String): Unit =
        client.put()
            .uri("/api/notifications/read")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    fun markAllRead(token: String): Unit =
        client.put()
            .uri("/api/notifications/read-all")
            .header("Authorization", token)
            .retrieve()
            .toBodilessEntity()
            .let {}
}
