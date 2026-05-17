package notification.api.public_notification

import notification.dto.public_notification.CreatePublicNotificationRequest
import notification.dto.public_notification.DeactivatePublicNotificationRequest
import notification.dto.public_notification.PublicNotificationResponse
import notification.service.public_notification.PublicNotificationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicNotificationController(private val publicNotificationService: PublicNotificationService) {

    @PostMapping("/api/public-notifications/list")
    fun listActive(): List<PublicNotificationResponse> =
        publicNotificationService.listActive()

    @PostMapping("/api/public-notifications")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreatePublicNotificationRequest): PublicNotificationResponse =
        publicNotificationService.create(request)

    @PutMapping("/api/public-notifications/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@RequestBody request: DeactivatePublicNotificationRequest) {
        publicNotificationService.deactivate(request)
    }
}
