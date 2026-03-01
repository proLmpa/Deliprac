package com.example.baemin.store.repository

import com.example.baemin.store.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByUserId(userId: Long): Store?
    fun existsByUserId(userId: Long): Boolean
}
