package com.example.baemin.service.store

import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.store.CreateStoreCommand
import com.example.baemin.dto.store.StoreInfo
import com.example.baemin.dto.store.UpdateStoreCommand
import com.example.baemin.entity.store.Store
import com.example.baemin.entity.store.StoreStatus
import com.example.baemin.repository.store.StoreRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class StoreService(
    private val storeRepository: StoreRepository
) {

    @Transactional
    fun create(command: CreateStoreCommand, principal: UserPrincipal): StoreInfo {
        if (principal.role != UserRole.OWNER) {
            throw IllegalStateException("Only OWNER can create a store")
        }
        if (storeRepository.existsByUserIdAndName(principal.id, command.name)) {
            throw IllegalStateException("Store with that name already exists")
        }
        val now = System.currentTimeMillis()
        val store = Store(
            id                 = 0L,
            userId             = principal.id,
            name               = command.name,
            address            = command.address,
            phone              = command.phone,
            content            = command.content,
            status             = StoreStatus.ACTIVE,
            storePictureUrl    = command.storePictureUrl,
            productCreatedTime = command.productCreatedTime,
            openedTime         = command.openedTime,
            closedTime         = command.closedTime,
            closedDays         = command.closedDays,
            createdAt          = now,
            updatedAt          = now
        )
        return StoreInfo.of(storeRepository.save(store))
    }

    @Transactional
    fun listAll(): List<StoreInfo> =
        storeRepository.findAll().map { StoreInfo.of(it) }

    @Transactional
    fun findById(id: Long): StoreInfo {
        val store = storeRepository.findById(id).orThrow("Store not found")
        return StoreInfo.of(store)
    }

    @Transactional
    fun findMine(principal: UserPrincipal): List<StoreInfo> {
        if (principal.role != UserRole.OWNER) {
            throw IllegalStateException("Only OWNER can access this")
        }
        return storeRepository.findByUserId(principal.id).map { StoreInfo.of(it) }
    }

    @Transactional
    fun update(id: Long, command: UpdateStoreCommand, principal: UserPrincipal): StoreInfo {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.userId != principal.id) {
            throw IllegalStateException("Forbidden")
        }
        store.name               = command.name
        store.address            = command.address
        store.phone              = command.phone
        store.content            = command.content
        store.storePictureUrl    = command.storePictureUrl
        store.productCreatedTime = command.productCreatedTime
        store.openedTime         = command.openedTime
        store.closedTime         = command.closedTime
        store.closedDays         = command.closedDays
        store.updatedAt          = System.currentTimeMillis()
        return StoreInfo.of(storeRepository.save(store))
    }

    @Transactional
    fun deactivate(id: Long, principal: UserPrincipal) {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.userId != principal.id) {
            throw IllegalStateException("Forbidden")
        }
        store.status    = StoreStatus.INACTIVE
        store.updatedAt = System.currentTimeMillis()
        storeRepository.save(store)
    }
}
