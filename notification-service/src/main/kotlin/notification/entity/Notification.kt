package notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
open class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Long = System.currentTimeMillis(),

    @Column(nullable = false)
    val expiry: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(expiry >= issuedAt + MIN_EXPIRY_MILLIS) {
            "Expiry must be at least 10 minutes after issuedAt"
        }
    }

    companion object {
        const val MIN_EXPIRY_MILLIS = 10 * 60 * 1000L
    }
}
