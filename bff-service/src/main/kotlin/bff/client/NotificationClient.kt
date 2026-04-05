package bff.client

import bff.dto.NotificationResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class MarkReadRequest(val notificationId: Long)

@Component
class NotificationClient(@Qualifier("notificationRestClient") private val client: RestClient) {

    fun listMyNotifications(token: String): List<NotificationResponse> =
        client.post()
            .uri("/api/notifications/me")
            .header("Authorization", token)
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
}
