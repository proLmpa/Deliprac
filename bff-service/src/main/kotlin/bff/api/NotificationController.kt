package bff.api

import bff.client.MarkReadRequest
import bff.client.NotificationClient
import bff.dto.NotificationResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(private val notificationClient: NotificationClient) {

    @PostMapping("/api/notifications/me")
    fun listMyNotifications(request: HttpServletRequest): List<NotificationResponse> =
        notificationClient.listMyNotifications(request.getHeader("Authorization"))

    @PutMapping("/api/notifications/read")
    fun markRead(@RequestBody body: MarkReadRequest, request: HttpServletRequest): Unit =
        notificationClient.markRead(body, request.getHeader("Authorization"))
}
