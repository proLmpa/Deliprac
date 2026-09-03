package order.service.order

import common.event.OrderEvent
import common.event.OrderEventItem
import common.event.OrderEventType
import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.orThrow
import common.security.UserRole
import common.security.currentUser
import order.dto.order.OrderResponse
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.order.OrderRepository
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

private val auditLog = LoggerFactory.getLogger("audit")

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartProductRepository: CartProductRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderEvent>
) {

    @Transactional(readOnly = true)
    fun listByStore(storeId: Long, role: UserRole): List<OrderResponse> {
        if (role != UserRole.OWNER)
            throw ForbiddenException("Forbidden")

        return orderRepository.findAllByStoreId(storeId).map { OrderResponse.of(it) }
    }

    @CacheEvict(value = ["orders-by-user"], allEntries = true)
    @Transactional
    fun markSold(storeId: Long, orderId: Long, role: UserRole): OrderResponse {
        if (role != UserRole.OWNER) throw ForbiddenException("Forbidden")

        val order = orderRepository.findById(orderId).orThrow("Not found")
        if (order.storeId != storeId) throw NotFoundException("Not found")
        if (order.status != OrderStatus.PENDING) throw ConflictException("Invalid operation")

        order.status = OrderStatus.SOLD
        val saved = orderRepository.save(order)
        val items = cartProductRepository.findAllByCartId(saved.cartId)

        kafkaTemplate.send(
            "baemin.order.events",
            saved.storeId.toString(),
            OrderEvent(
                eventType    = OrderEventType.ORDER_SOLD,
                orderId      = saved.id,
                userId       = saved.userId,
                storeId      = saved.storeId,
                storeOwnerId = saved.storeOwnerId,
                storeName    = saved.storeName,
                totalPrice   = saved.totalPrice,
                items        = items.map { OrderEventItem(it.productId, "", it.quantity, it.unitPrice) }
            )
        )

        auditLog.info(
            appendEntries(mapOf("event" to "ORDER_CONFIRMED", "orderId" to saved.id, "storeId" to storeId, "email" to currentUser().email)),
            "Owner ${currentUser().email} confirmed order ${saved.id} at store $storeId"
        )
        return OrderResponse.of(saved, items)
    }

    @CacheEvict(value = ["orders-by-user"], allEntries = true)
    @Transactional
    fun markCanceled(storeId: Long, orderId: Long, role: UserRole): OrderResponse {
        if (role != UserRole.OWNER) throw ForbiddenException("Forbidden")

        val order = orderRepository.findById(orderId).orThrow("Not found")
        if (order.storeId != storeId) throw NotFoundException("Not found")
        if (order.status != OrderStatus.PENDING) throw ConflictException("Invalid operation")

        order.status = OrderStatus.CANCELED
        val saved = orderRepository.save(order)
        val items = cartProductRepository.findAllByCartId(saved.cartId)

        kafkaTemplate.send(
            "baemin.order.events",
            saved.storeId.toString(),
            OrderEvent(
                eventType    = OrderEventType.ORDER_CANCELLED,
                orderId      = saved.id,
                userId       = saved.userId,
                storeId      = saved.storeId,
                storeOwnerId = saved.storeOwnerId,
                storeName    = saved.storeName,
                totalPrice   = saved.totalPrice,
                items        = items.map { OrderEventItem(it.productId, "", it.quantity, it.unitPrice) }
            )
        )

        auditLog.info(
            appendEntries(mapOf("event" to "ORDER_CANCELLED", "orderId" to saved.id, "storeId" to storeId, "email" to currentUser().email)),
            "Owner ${currentUser().email} cancelled order ${saved.id} at store $storeId"
        )
        return OrderResponse.of(saved, items)
    }

    @Cacheable(value = ["orders-by-user"], key = "#userId")
    @Transactional(readOnly = true)
    fun listByUser(userId: Long): List<OrderResponse> =
        orderRepository.findAllByUserId(userId).map { OrderResponse.of(it) }

    @Transactional(readOnly = true)
    fun getById(orderId: Long): OrderResponse =
        OrderResponse.of(orderRepository.findById(orderId).orThrow("Not found"))
}
