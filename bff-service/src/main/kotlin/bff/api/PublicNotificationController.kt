package bff.api

import bff.client.NotificationClient
import bff.dto.CreatePublicNotificationRequest
import bff.dto.DeactivatePublicNotificationRequest
import bff.dto.PublicNotificationResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicNotificationController(private val notificationClient: NotificationClient) {

    @PostMapping("/api/public-notifications/list")
    fun listPublicNotifications(httpRequest: HttpServletRequest): List<PublicNotificationResponse> =
        notificationClient.listPublicNotifications(httpRequest.bearerToken())

    @PostMapping("/api/public-notifications")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreatePublicNotificationRequest,
        httpRequest: HttpServletRequest,
    ): PublicNotificationResponse =
        notificationClient.createPublicNotification(request, httpRequest.bearerToken())

    @PutMapping("/api/public-notifications/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(
        @RequestBody request: DeactivatePublicNotificationRequest,
        httpRequest: HttpServletRequest,
    ) {
        notificationClient.deactivatePublicNotification(request, httpRequest.bearerToken())
    }
}
