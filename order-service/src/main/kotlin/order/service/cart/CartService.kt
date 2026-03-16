package order.service.cart

import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
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
        val activeCart = cartRepository.findByUserIdAndIsOrderedFalse(userId)

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

        return CartInfo(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional(readOnly = true)
    fun getMyCart(userId: Long): CartInfo {
        val cart = cartRepository.findByUserIdAndIsOrderedFalse(userId)
            ?: throw NotFoundException("Cart not found")

        return CartInfo(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional
    fun removeItem(cartId: Long, productId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")

        cartProductRepository.deleteByCartIdAndProductId(cartId, productId)
        cartRepository.save(cart)
    }

    @Transactional
    fun clearCart(cartId: Long, userId: Long) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")

        cartProductRepository.deleteByCartId(cartId)
        cartRepository.save(cart)
    }

    @Transactional
    fun checkout(cartId: Long, userId: Long): Order {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != userId) throw ForbiddenException("Forbidden")
        if (cart.isOrdered or orderRepository.existsByCartId(cartId)) throw ConflictException("Cart already checked out")

        val items = cartProductRepository.findAllByCartId(cartId)
        if (items.isEmpty()) throw ConflictException("Cart is empty")

        val order = orderRepository.save(
            Order(0L, cartId, userId, cart.storeId, items.sumOf { it.unitPrice * it.quantity }, OrderStatus.PENDING)
        )

        cart.isOrdered = true
        cartRepository.save(cart)

        return order
    }

    private fun createCart(userId: Long, storeId: Long): Cart =
        cartRepository.save(Cart(0L, userId, storeId, false))

    private fun resetCart(cart: Cart, newStoreId: Long): Cart {
        cartProductRepository.deleteByCartId(cart.id)
        cart.storeId = newStoreId
        return cartRepository.save(cart)
    }
}
