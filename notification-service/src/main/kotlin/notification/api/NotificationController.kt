package notification.api

import common.security.currentUser
import notification.dto.MarkReadRequest
import notification.dto.NotificationResponse
import notification.service.NotificationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(private val notificationService: NotificationService) {

    @PostMapping("/api/notifications/me")
    fun listMyNotifications(): List<NotificationResponse> =
        notificationService.listByUser(currentUser().id)

    @PutMapping("/api/notifications/read")
    fun markRead(@RequestBody request: MarkReadRequest) =
        notificationService.markRead(request.notificationId, currentUser().id)
}
