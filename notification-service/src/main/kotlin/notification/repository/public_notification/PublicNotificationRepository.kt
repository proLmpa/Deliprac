package notification.repository.public_notification

import notification.entity.public_notification.PublicNotification
import org.springframework.data.jpa.repository.JpaRepository

interface PublicNotificationRepository : JpaRepository<PublicNotification, Long> {
    fun findAllByIsActiveTrue(): List<PublicNotification>
}
