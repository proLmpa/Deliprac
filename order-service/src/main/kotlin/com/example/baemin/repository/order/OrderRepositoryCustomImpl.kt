package com.example.baemin.repository.order

import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.entity.order.QOrder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.YearMonth
import java.time.ZoneId

@Repository
class OrderRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrderRepositoryCustom {

    private fun monthRange(year: Int, month: Int, zoneId: ZoneId): Pair<Long, Long> {
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end   = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    override fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int, zoneId: ZoneId): Int {
        val order = QOrder.order
        val (start, end) = monthRange(year, month, zoneId)
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

    override fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int, zoneId: ZoneId): Int {
        val order = QOrder.order
        val (start, end) = monthRange(year, month, zoneId)
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
