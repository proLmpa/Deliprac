package order.repository.order

import order.entity.order.OrderStatus
import order.entity.order.QOrder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.YearMonth
import java.time.ZoneId

@Repository
class OrderRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrderRepositoryCustom {

    private fun monthRange(year: Int, month: Int, zoneId: ZoneId): LongRange {
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end   = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start until end
    }

    override fun sumRevenueByStoreAndMonth(storeId: Long, year: Int, month: Int, zoneId: ZoneId): Long {
        val order = QOrder.order
        val range = monthRange(year, month, zoneId)
        return queryFactory
            .select(order.totalPrice.sum())
            .from(order)
            .where(
                order.storeId.eq(storeId),
                order.status.eq(OrderStatus.SOLD),
                order.createdAt.goe(range.start),
                order.createdAt.lt(range.last)
            )
            .fetchOne() ?: 0L
    }

    override fun sumSpendingByUserAndMonth(userId: Long, year: Int, month: Int, zoneId: ZoneId): Long {
        val order = QOrder.order
        val range = monthRange(year, month, zoneId)
        return queryFactory
            .select(order.totalPrice.sum())
            .from(order)
            .where(
                order.userId.eq(userId),
                order.status.eq(OrderStatus.SOLD),
                order.createdAt.goe(range.first),
                order.createdAt.lt(range.last)
            )
            .fetchOne() ?: 0L
    }
}
