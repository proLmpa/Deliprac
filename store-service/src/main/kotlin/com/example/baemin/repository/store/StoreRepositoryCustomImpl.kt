package com.example.baemin.repository.store

import com.example.baemin.entity.store.QStore
import com.example.baemin.entity.store.Store
import com.example.baemin.entity.store.StoreStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class StoreRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : StoreRepositoryCustom {

    override fun findActiveStoresOrderByCreatedAtDesc(): List<Store> {
        val store = QStore.store
        return queryFactory
            .selectFrom(store)
            .where(store.status.eq(StoreStatus.ACTIVE))
            .orderBy(store.createdAt.desc())
            .fetch()
    }
}
