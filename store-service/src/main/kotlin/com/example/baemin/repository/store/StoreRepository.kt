package com.example.baemin.repository.store

import com.example.baemin.entity.store.Store
import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByUserId(userId: Long): Store?
    fun existsByUserId(userId: Long): Boolean
}
