package com.example.baemin.repository.store

import com.example.baemin.entity.store.Store

interface StoreRepositoryCustom {
    fun findActiveStoresOrderByCreatedAtDesc(): List<Store>
}
