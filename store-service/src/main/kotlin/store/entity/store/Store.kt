package store.entity.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "stores")
open class Store(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StoreStatus,

    @Column(name = "store_picture_url", length = 500)
    var storePictureUrl: String? = null,

    @Column(name = "product_created_time", nullable = false)
    var productCreatedTime: Long,

    @Column(name = "opened_time", nullable = false)
    var openedTime: Long,

    @Column(name = "closed_time", nullable = false)
    var closedTime: Long,

    @Column(name = "closed_days", nullable = false, length = 50)
    var closedDays: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long
)
