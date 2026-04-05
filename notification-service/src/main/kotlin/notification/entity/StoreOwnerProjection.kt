package notification.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "store_owner_projections")
open class StoreOwnerProjection(
    @Id
    open var storeId: Long = 0L,
    open var ownerUserId: Long = 0L
)
