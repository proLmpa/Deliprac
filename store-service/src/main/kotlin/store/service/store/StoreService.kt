package store.service.store

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.orThrow
import common.security.UserPrincipal
import common.security.UserRole
import store.dto.store.CreateStoreCommand
import store.dto.store.StoreInfo
import store.dto.store.StoreSortBy
import store.dto.store.UpdateStoreCommand
import store.entity.store.Store
import store.entity.store.StoreStatus
import store.repository.review.ReviewRepository
import store.repository.store.StoreRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class StoreService(
    private val storeRepository: StoreRepository,
    private val reviewRepository: ReviewRepository,
) {

    @Transactional
    fun create(command: CreateStoreCommand, principal: UserPrincipal): StoreInfo {
        if (principal.role != UserRole.OWNER) {
            throw ForbiddenException("Only OWNER can create a store")
        }
        if (storeRepository.existsByUserIdAndName(principal.id, command.name)) {
            throw ConflictException("Store with that name already exists")
        }

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
        )

        val saved = storeRepository.save(store)
        return StoreInfo.of(saved, 0.0)
    }

    @Transactional(readOnly = true)
    fun listAll(sortBy: StoreSortBy): List<StoreInfo> {
        val stores = storeRepository.findActiveStoresOrderByCreatedAtDesc()
        if (stores.isEmpty()) return emptyList()

        val ratings = reviewRepository.calculateAverageRatingsForStores(stores.map { it.id })

        val sorted = when (sortBy) {
            StoreSortBy.CREATED_AT -> stores
            StoreSortBy.RATING     -> stores.sortedByDescending { ratings[it.id] ?: 0.0 }
        }

        return sorted.map { StoreInfo.of(it, ratings[it.id] ?: 0.0) }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): StoreInfo {
        val store = storeRepository.findById(id).orThrow("Store not found")
        val rating = reviewRepository.calculateAverageRatingByStoreId(id)
        return StoreInfo.of(store, rating)
    }

    @Transactional(readOnly = true)
    fun findMine(principal: UserPrincipal): List<StoreInfo> {
        if (principal.role != UserRole.OWNER) {
            throw ForbiddenException("Only OWNER can access this")
        }

        val stores = storeRepository.findByUserId(principal.id)
        if (stores.isEmpty()) return emptyList()

        val ratings = reviewRepository.calculateAverageRatingsForStores(stores.map { it.id })
        return stores.map { StoreInfo.of(it, ratings[it.id] ?: 0.0) }
    }

    @Transactional
    fun update(id: Long, command: UpdateStoreCommand, userId: Long): StoreInfo {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.userId != userId) {
            throw ForbiddenException("Forbidden")
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

        val rating = reviewRepository.calculateAverageRatingByStoreId(id)
        return StoreInfo.of(storeRepository.save(store), rating)
    }

    @Transactional
    fun deactivate(id: Long, userId: Long) {
        val store = storeRepository.findById(id).orThrow("Store not found")
        if (store.userId != userId) {
            throw ForbiddenException("Forbidden")
        }

        store.status = StoreStatus.INACTIVE

        storeRepository.save(store)
    }
}
