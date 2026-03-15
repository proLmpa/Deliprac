package order.entity.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
open class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(name = "cart_id", nullable = false, unique = true)
    val cartId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "total_price", nullable = false)
    val totalPrice: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long
)
