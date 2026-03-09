package com.example.baemin.repository.store

import com.example.baemin.entity.store.Store
import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long>, StoreRepositoryCustom {
    fun findByUserId(userId: Long): List<Store>
    fun existsByUserIdAndName(userId: Long, name: String): Boolean
}
