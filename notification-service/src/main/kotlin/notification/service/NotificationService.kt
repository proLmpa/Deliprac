package notification.service

import common.exception.ForbiddenException
import common.orThrow
import notification.dto.CreateNotificationRequest
import notification.dto.NotificationResponse
import notification.entity.Notification
import notification.entity.NotificationItemData
import notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(private val notificationRepository: NotificationRepository) {

    @Transactional
    fun createNotification(request: CreateNotificationRequest): NotificationResponse {
        val now = System.currentTimeMillis()
        return NotificationResponse.of(notificationRepository.save(
            Notification(
                userId    = request.recipientId,
                type      = request.type,
                title     = request.title,
                content   = request.content,
                storeId   = request.storeId,
                storeName = request.storeName,
                items     = request.items.map { NotificationItemData(it.productName, it.unitPrice, it.quantity) },
                issuedAt  = now,
                expiry    = request.expiry,
                createdAt = now
            )
        ))
    }

    @Transactional(readOnly = true)
    fun listMyNotifications(userId: Long, unreadOnly: Boolean): List<NotificationResponse> =
        (if (unreadOnly) notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        else notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .map { NotificationResponse.of(it) }

    @Transactional
    fun markRead(userId: Long, notificationId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId).orThrow("Notification not found")
        if (notification.userId != userId) throw ForbiddenException("Forbidden")
        notification.isRead = true
        return NotificationResponse.of(notificationRepository.save(notification))
    }

    @Transactional
    fun markAllRead(userId: Long) {
        val unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        unread.forEach { it.isRead = true }
        notificationRepository.saveAll(unread)
    }
}
