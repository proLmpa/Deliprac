package notification.service

import common.exception.ForbiddenException
import common.orThrow
import notification.dto.CreateNotificationRequest
import notification.entity.Notification
import notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(private val notificationRepository: NotificationRepository) {

    @Transactional
    fun createNotification(request: CreateNotificationRequest): Notification {
        val now = System.currentTimeMillis()
        return notificationRepository.save(
            Notification(
                userId    = request.recipientId,
                title     = request.title,
                content   = request.content,
                issuedAt  = now,
                expiry    = request.expiry,
                createdAt = now
            )
        )
    }

    @Transactional(readOnly = true)
    fun listMyNotifications(userId: Long, unreadOnly: Boolean): List<Notification> =
        if (unreadOnly) notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        else notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)

    @Transactional
    fun markRead(userId: Long, notificationId: Long): Notification {
        val notification = notificationRepository.findById(notificationId).orThrow("Notification not found")
        if (notification.userId != userId) throw ForbiddenException("Forbidden")
        notification.isRead = true
        return notificationRepository.save(notification)
    }

    @Transactional
    fun markAllRead(userId: Long) {
        val unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        unread.forEach { it.isRead = true }
        notificationRepository.saveAll(unread)
    }
}
