package store.repository.store

import store.entity.store.Store

interface StoreRepositoryCustom {
    fun findActiveStoresOrderByCreatedAtDesc(): List<Store>
}
