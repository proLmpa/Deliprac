package com.example.baemin.store.repository

import com.example.baemin.store.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoreRepository : JpaRepository<Store, UUID> {
    fun findByOwnerId(ownerId: UUID): Store?
    fun existsByOwnerId(ownerId: UUID): Boolean
}
