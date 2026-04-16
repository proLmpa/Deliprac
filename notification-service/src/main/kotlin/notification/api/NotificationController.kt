package notification.api

import common.security.currentUser
import notification.dto.CreateNotificationRequest
import notification.dto.ListNotificationRequest
import notification.dto.MarkReadRequest
import notification.dto.NotificationResponse
import notification.service.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(private val notificationService: NotificationService) {

    @PostMapping("/internal/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateNotificationRequest): NotificationResponse =
        notificationService.createNotification(request)

    @PostMapping("/api/notifications/list")
    fun listMy(@RequestBody request: ListNotificationRequest): List<NotificationResponse> =
        notificationService.listMyNotifications(currentUser().id, request.unreadOnly)

    @PutMapping("/api/notifications/read")
    fun markRead(@RequestBody request: MarkReadRequest): NotificationResponse =
        notificationService.markRead(currentUser().id, request.notificationId)

    @PutMapping("/api/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllRead() {
        notificationService.markAllRead(currentUser().id)
    }
}
