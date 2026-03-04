package com.example.baemin.service.cart

import com.example.baemin.client.StoreServiceClient
import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.dto.cart.AddCartItemRequest
import com.example.baemin.dto.cart.CartResponse
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.entity.cart.Cart
import com.example.baemin.entity.cart.CartProduct
import com.example.baemin.entity.order.Order
import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.repository.cart.CartProductRepository
import com.example.baemin.repository.cart.CartRepository
import com.example.baemin.repository.order.OrderRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartProductRepository: CartProductRepository,
    private val orderRepository: OrderRepository,
    private val storeServiceClient: StoreServiceClient
) {

    @Transactional
    fun addItem(request: AddCartItemRequest, principal: UserPrincipal): CartResponse {
        val product = storeServiceClient.getProduct(request.productId)
        if (!product.status) throw IllegalArgumentException("Product is not available")

        val now = System.currentTimeMillis()
        val existingCart = cartRepository.findByUserId(principal.id)

        val cart: Cart = when {
            existingCart == null -> {
                cartRepository.save(Cart(0L, principal.id, product.storeId, false, now, now))
            }
            existingCart.isOrdered || existingCart.storeId != product.storeId -> {
                // Checked-out cart or different store — reset to a fresh cart
                cartProductRepository.deleteByCartId(existingCart.id)
                existingCart.storeId   = product.storeId
                existingCart.isOrdered = false
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
            cartProductRepository.save(CartProduct(0L, cart.id, request.productId, request.quantity, product.price))
        }

        return CartResponse.of(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional
    fun getMyCart(principal: UserPrincipal): CartResponse {
        val cart = cartRepository.findByUserId(principal.id)
            ?: throw IllegalArgumentException("Cart not found")
        return CartResponse.of(cart, cartProductRepository.findAllByCartId(cart.id))
    }

    @Transactional
    fun removeItem(cartId: Long, productId: Long, principal: UserPrincipal) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != principal.id) throw IllegalStateException("Forbidden")
        cartProductRepository.deleteByCartIdAndProductId(cartId, productId)
        cart.updatedAt = System.currentTimeMillis()
        cartRepository.save(cart)
    }

    @Transactional
    fun clearCart(cartId: Long, principal: UserPrincipal) {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != principal.id) throw IllegalStateException("Forbidden")
        cartProductRepository.deleteByCartId(cartId)
        cart.updatedAt = System.currentTimeMillis()
        cartRepository.save(cart)
    }

    @Transactional
    fun checkout(cartId: Long, principal: UserPrincipal): OrderResponse {
        val cart = cartRepository.findById(cartId).orThrow("Cart not found")
        if (cart.userId != principal.id) throw IllegalStateException("Forbidden")
        if (cart.isOrdered) throw IllegalStateException("Cart already checked out")

        val items = cartProductRepository.findAllByCartId(cartId)
        if (items.isEmpty()) throw IllegalArgumentException("Cart is empty")

        val now = System.currentTimeMillis()
        val order = orderRepository.save(
            Order(
                id         = 0L,
                cartId     = cartId,
                userId     = principal.id,
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

        return OrderResponse.of(order)
    }
}
