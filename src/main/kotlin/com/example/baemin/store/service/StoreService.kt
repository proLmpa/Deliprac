package com.example.baemin.store.service

import com.example.baemin.common.orThrow
import com.example.baemin.store.dto.CreateStoreRequest
import com.example.baemin.store.dto.StoreResponse
import com.example.baemin.store.dto.UpdateStoreRequest
import com.example.baemin.store.entity.Store
import com.example.baemin.store.repository.StoreRepository
import com.example.baemin.user.entity.UserRole
import com.example.baemin.user.repository.UserRepository
import com.example.baemin.user.security.UserPrincipal
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class StoreService(
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(request: CreateStoreRequest, principal: UserPrincipal): StoreResponse {
        if (principal.role != UserRole.OWNER) {
            throw IllegalStateException("Only OWNER can create a store")
        }
        if (storeRepository.existsByOwnerId(principal.id)) {
            throw IllegalStateException("Store already exists for this owner")
        }
        val owner = userRepository.findById(principal.id).orThrow("User not found")
        val store = Store(
            owner              = owner,
            name               = request.name,
            address            = request.address,
            phone              = request.phone,
            content            = request.content,
            storePictureUrl    = request.storePictureUrl,
            productCreatedTime = request.productCreatedTime,
            openedTime         = request.openedTime,
            closedTime         = request.closedTime,
            closedDays         = request.closedDays
        )
        return StoreResponse.from(storeRepository.save(store))
    }

    @Transactional
    fun listAll(): List<StoreResponse> =
        storeRepository.findAll().map { StoreResponse.from(it) }

    @Transactional
    fun findById(id: UUID): StoreResponse {
        val store = storeRepository.findById(id).orThrow("Store not found")
        return StoreResponse.from(store)
    }

    @Transactional
    fun findMine(principal: UserPrincipal): StoreResponse {
        if (principal.role != UserRole.OWNER) {
            throw IllegalStateException("Only OWNER can access this")
        }
        val store = storeRepository.findByOwnerId(principal.id)
            ?: throw IllegalArgumentException("Store not found")
        return StoreResponse.from(store)
    }

    @Transactional
    fun update(id: UUID, request: UpdateStoreRequest, principal: UserPrincipal): StoreResponse {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.owner.id != principal.id) {
            throw IllegalStateException("Forbidden")
        }
        store.name               = request.name
        store.address            = request.address
        store.phone              = request.phone
        store.content            = request.content
        store.storePictureUrl    = request.storePictureUrl
        store.productCreatedTime = request.productCreatedTime
        store.openedTime         = request.openedTime
        store.closedTime         = request.closedTime
        store.closedDays         = request.closedDays
        store.updatedAt          = Instant.now()
        return StoreResponse.from(storeRepository.save(store))
    }

    @Transactional
    fun delete(id: UUID, principal: UserPrincipal) {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.owner.id != principal.id) {
            throw IllegalStateException("Forbidden")
        }
        storeRepository.delete(store)
    }
}
