package notification.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
open class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0L,
    open var userId: Long = 0L,
    open var title: String = "",
    open var content: String = "",
    open var isRead: Boolean = false,
    open var createdAt: Long = System.currentTimeMillis()
)
