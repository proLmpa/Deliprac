package bff.client

import bff.dto.ListNotificationRequest
import bff.dto.MarkReadRequest
import bff.dto.NotificationResponse
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(NotificationClient::class.java)

    @CircuitBreaker(name = "notification", fallbackMethod = "createNotificationFallback")
    fun createNotification(request: CreateNotificationRequest) {
        client.post()
            .uri("/internal/notifications")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    private fun createNotificationFallback(request: CreateNotificationRequest, ex: Throwable) {
        log.warn("[CB] createNotification skipped ({}): {}", ex.javaClass.simpleName, ex.message)
    }

    @CircuitBreaker(name = "notification", fallbackMethod = "listMyNotificationsFallback")
    fun listMyNotifications(request: ListNotificationRequest, token: String): List<NotificationResponse> =
        client.post()
            .uri("/api/notifications/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<NotificationResponse>>() {})!!

    private fun listMyNotificationsFallback(request: ListNotificationRequest, token: String, ex: Throwable): List<NotificationResponse> {
        log.warn("[CB] listMyNotifications fallback: {}", ex.message)
        return emptyList()
    }

    @CircuitBreaker(name = "notification", fallbackMethod = "markReadFallback")
    fun markRead(request: MarkReadRequest, token: String): Unit =
        client.put()
            .uri("/api/notifications/read")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    private fun markReadFallback(request: MarkReadRequest, token: String, ex: Throwable) {
        log.warn("[CB] markRead fallback: {}", ex.message)
    }

    @CircuitBreaker(name = "notification", fallbackMethod = "markAllReadFallback")
    fun markAllRead(token: String): Unit =
        client.put()
            .uri("/api/notifications/read-all")
            .header("Authorization", token)
            .retrieve()
            .toBodilessEntity()
            .let {}

    private fun markAllReadFallback(token: String, ex: Throwable) {
        log.warn("[CB] markAllRead fallback: {}", ex.message)
    }
}
