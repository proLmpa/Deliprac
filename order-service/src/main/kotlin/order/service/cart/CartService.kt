package order.service.cart

import common.event.OrderEvent
import common.event.OrderEventItem
import common.event.OrderEventType
import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.orThrow
import common.security.currentUser
import order.dto.cart.AddCartItemRequest
import order.dto.cart.CartResponse
import order.dto.order.OrderResponse
import order.entity.cart.Cart
import order.entity.cart.CartProduct
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.cart.CartRepository
import order.repository.order.OrderRepository
import net.logstash.logback.marker.Markers.appendEntries
import order.dto.cart.CheckoutRequest
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val auditLog = LoggerFactory.getLogger("audit")

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartProductRepository: CartProductRepository,
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderEvent>
) {

    @Transactional
    fun addItem(request: AddCartItemRequest, userId: Long): CartResponse {
        val activeCart = cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)

        val cart: Cart = when {
            activeCart == null                    -> createCart(userId, request.storeId)
            activeCart.storeId != request.storeId -> resetCart(activeCart, request.storeId)
            else                                  -> activeCart
        }

        val existing = cartProductRepository.findByCartIdAndProductId(cart.id, request.productId)
        if (existing != null) {
            existing.quantity += request.quantity
            cartProductRepository.save(existing)
        } else {
            cartProductRepository.save(CartProduct(0L, cart.id, request.productId, request.quantity, request.unitPrice))
        }

        return CartResponse.of(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional(readOnly = true)
    fun getMyCart(userId: Long): CartResponse {
        val cart = cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)
            ?: throw NotFoundException("Not found")

        return CartResponse.of(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional
    fun removeItem(cartId: Long, productId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")

        cartProductRepository.deleteByCartIdAndProductId(cartId, productId)
        cartRepository.save(cart)
    }

    @Transactional
    fun clearCart(cartId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")

        cartProductRepository.deleteByCartId(cartId)
        cartRepository.save(cart)
    }

    @CacheEvict(value = ["orders-by-user"], key = "#userId")
    @Transactional
    fun checkout(request: CheckoutRequest, userId: Long): OrderResponse {
        val cart = cartRepository.findById(request.cartId).orThrow("Not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")
        if (cart.isOrdered or orderRepository.existsByCartId(request.cartId)) throw ConflictException("Invalid operation")

        val items = cartProductRepository.findAllByCartId(request.cartId)
        if (items.isEmpty()) throw ConflictException("Invalid operation")

        val order = orderRepository.save(
            Order(0L, request.cartId, userId, cart.storeId, items.sumOf { it.unitPrice * it.quantity }, request.storeOwnerId, request.storeName, OrderStatus.PENDING)
        )

        cart.isOrdered = true
        cartRepository.save(cart)

        val event = OrderEvent(
            eventType = OrderEventType.NEW_ORDER,
            orderId = order.id,
            userId = userId,
            storeId = order.storeId,
            storeOwnerId = request.storeOwnerId,
            storeName = request.storeName,
            totalPrice = order.totalPrice,
            items = items.map { cp ->
                val meta = request.items.find { it.productId == cp.productId }
                OrderEventItem(cp.productId, meta?.productName ?: "", cp.quantity, cp.unitPrice)
            }
        )
        kafkaTemplate.send("baemin.order.events", order.storeId.toString(), event)

        auditLog.info(
            appendEntries(mapOf("event" to "ORDER_CREATED", "orderId" to order.id, "storeId" to order.storeId, "totalPrice" to order.totalPrice, "email" to currentUser().email)),
            "Customer ${currentUser().email} placed order ${order.id} at store ${order.storeId} (total: ${order.totalPrice})"
        )
        return OrderResponse.of(order, items)
    }

    private fun createCart(userId: Long, storeId: Long): Cart =
        cartRepository.save(Cart(0L, userId, storeId, false))

    private fun resetCart(cart: Cart, newStoreId: Long): Cart {
        cartProductRepository.deleteByCartId(cart.id)
        cart.storeId = newStoreId
        return cartRepository.save(cart)
    }
}
