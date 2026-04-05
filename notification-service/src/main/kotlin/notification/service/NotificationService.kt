package notification.service

import common.exception.ForbiddenException
import common.orThrow
import notification.dto.NotificationResponse
import notification.entity.Notification
import notification.repository.NotificationRepository
import notification.websocket.NotificationWebSocketHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val webSocketHandler: NotificationWebSocketHandler
) {

    @Transactional
    fun create(userId: Long, title: String, content: String): NotificationResponse {
        val notification = notificationRepository.save(
            Notification(userId = userId, title = title, content = content)
        )
        val response = NotificationResponse.of(notification)
        webSocketHandler.push(userId, response)
        return response
    }

    @Transactional(readOnly = true)
    fun listByUser(userId: Long): List<NotificationResponse> =
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { NotificationResponse.of(it) }

    @Transactional
    fun markRead(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId).orThrow("Notification not found")
        if (notification.userId != userId) throw ForbiddenException("Forbidden")
        notification.isRead = true
        notificationRepository.save(notification)
    }
}
