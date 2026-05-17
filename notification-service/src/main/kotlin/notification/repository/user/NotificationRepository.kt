package notification.repository.user

import notification.entity.user.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
    fun findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long): List<Notification>
}
