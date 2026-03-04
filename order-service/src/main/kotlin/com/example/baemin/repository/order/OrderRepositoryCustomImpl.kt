package com.example.baemin.repository.order

import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.entity.order.QOrder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneOffset

@Repository
class OrderRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrderRepositoryCustom {

    override fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int): Int {
        val order = QOrder.order
        val start = LocalDate.of(year, month, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val end   = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return queryFactory
            .select(order.totalPrice.sum())
            .from(order)
            .where(
                order.storeId.eq(storeId),
                order.status.eq(OrderStatus.SOLD),
                order.createdAt.goe(start),
                order.createdAt.lt(end)
            )
            .fetchOne() ?: 0
    }

    override fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int): Int {
        val order = QOrder.order
        val start = LocalDate.of(year, month, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val end   = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return queryFactory
            .select(order.totalPrice.sum())
            .from(order)
            .where(
                order.userId.eq(userId),
                order.status.eq(OrderStatus.SOLD),
                order.createdAt.goe(start),
                order.createdAt.lt(end)
            )
            .fetchOne() ?: 0
    }
}
