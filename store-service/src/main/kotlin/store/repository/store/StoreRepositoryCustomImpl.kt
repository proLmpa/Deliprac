package store.repository.store

import store.entity.store.QStore
import store.entity.store.Store
import store.entity.store.StoreStatus
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
