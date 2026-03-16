package order.service.cart

import common.orThrow
import order.dto.cart.AddCartItemRequest
import order.dto.cart.CartInfo
import order.entity.cart.Cart
import order.entity.cart.CartProduct
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.cart.CartRepository
import order.repository.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartProductRepository: CartProductRepository,
    private val orderRepository: OrderRepository
) {

    @Transactional
    fun addItem(request: AddCartItemRequest, userId: Long): CartInfo {
        val now = System.currentTimeMillis()
        val existingCart = cartRepository.findByUserId(userId)

        val cart: Cart = when {
            existingCart == null -> {
                cartRepository.save(Cart(0L, userId, request.storeId, false, now, now))
            }
            existingCart.isOrdered -> {
                // Already checked out — the old cartId is permanently bound to an existing order.
                // Reusing it would violate orders_cart_id_key, so delete and create a fresh cart.
                cartProductRepository.deleteByCartId(existingCart.id)
                cartRepository.delete(existingCart)
                cartRepository.save(Cart(0L, userId, request.storeId, false, now, now))
            }
            existingCart.storeId != request.storeId -> {
                // Different store — clear items and reset in-place (no existing order, cartId is safe to reuse)
                cartProductRepository.deleteByCartId(existingCart.id)
                existingCart.storeId   = request.storeId
                existingCart.updatedAt = now
                cartRepository.save(existingCart)
            }
            else -> existingCart
        }

        val existing = cartProductRepository.findByCartIdAndProductId(cart.id, request.productId)
        if (existing != null) {
            existing.quantity += request.quantity
            cartProductRepository.save(existing)
        } else {
            cartProductRepository.save(CartProduct(0L, cart.id, request.productId, request.quantity, request.unitPrice))
        }

        return CartInfo(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional(readOnly = true)
    fun getMyCart(userId: Long): CartInfo {
        val cart = cartRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("Cart not found")

        return CartInfo(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional
    fun removeItem(cartId: Long, productId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw IllegalStateException("Forbidden")

        cartProductRepository.deleteByCartIdAndProductId(cartId, productId)
        cart.updatedAt = System.currentTimeMillis()
        cartRepository.save(cart)
    }

    @Transactional
    fun clearCart(cartId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw IllegalStateException("Forbidden")

        cartProductRepository.deleteByCartId(cartId)
        cart.updatedAt = System.currentTimeMillis()
        cartRepository.save(cart)
    }

    @Transactional
    fun checkout(cartId: Long, userId: Long): Order {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw IllegalStateException("Forbidden")
        if (cart.isOrdered or orderRepository.existsByCartId(cartId)) throw IllegalStateException("Cart already checked out")

        val items = cartProductRepository.findAllByCartId(cartId)
        if (items.isEmpty()) throw IllegalArgumentException("Cart is empty")

        val now = System.currentTimeMillis()
        val order = orderRepository.save(
            Order(
                id         = 0L,
                cartId     = cartId,
                userId     = userId,
                storeId    = cart.storeId,
                totalPrice = items.sumOf { it.unitPrice * it.quantity },
                status     = OrderStatus.PENDING,
                createdAt  = now,
                updatedAt  = now
            )
        )

        cart.isOrdered = true
        cart.updatedAt = now
        cartRepository.save(cart)

        return order
    }
}
