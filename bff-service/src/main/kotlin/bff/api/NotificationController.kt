package bff.api

import bff.client.NotificationClient
import bff.dto.ListNotificationRequest
import bff.dto.MarkReadRequest
import bff.dto.NotificationResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(private val notificationClient: NotificationClient) {

    @PostMapping("/api/notifications/list")
    fun listMyNotifications(
        @RequestBody request: ListNotificationRequest,
        httpRequest: HttpServletRequest
    ): List<NotificationResponse> =
        notificationClient.listMyNotifications(request, httpRequest.bearerToken())

    @PutMapping("/api/notifications/read")
    fun markRead(@RequestBody request: MarkReadRequest, httpRequest: HttpServletRequest): Unit =
        notificationClient.markRead(request, httpRequest.bearerToken())

    @PutMapping("/api/notifications/read-all")
    fun markAllRead(httpRequest: HttpServletRequest): Unit =
        notificationClient.markAllRead(httpRequest.bearerToken())
}
