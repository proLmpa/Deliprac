package notification.repository

import notification.entity.StoreOwnerProjection
import org.springframework.data.jpa.repository.JpaRepository

interface StoreOwnerProjectionRepository : JpaRepository<StoreOwnerProjection, Long>
