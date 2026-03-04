package com.example.baemin.service.cart

import com.example.baemin.client.RemoteProductInfo
import com.example.baemin.client.StoreServiceClient
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.cart.AddCartItemRequest
import com.example.baemin.entity.cart.Cart
import com.example.baemin.entity.cart.CartProduct
import com.example.baemin.entity.order.Order
import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.repository.cart.CartProductRepository
import com.example.baemin.repository.cart.CartRepository
import com.example.baemin.repository.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CartServiceTest {

    @Mock private lateinit var cartRepository: CartRepository
    @Mock private lateinit var cartProductRepository: CartProductRepository
    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var storeServiceClient: StoreServiceClient
    @InjectMocks private lateinit var cartService: CartService

    private val userId    = 1L
    private val storeId   = 10L
    private val cartId    = 50L
    private val productId = 100L
    private val principal = UserPrincipal(userId, "user@example.com", UserRole.CUSTOMER)

    private val activeProduct = RemoteProductInfo(storeId = storeId, price = 8000, status = true)
    private val inactiveProduct = RemoteProductInfo(storeId = storeId, price = 8000, status = false)

    private fun makeCart(isOrdered: Boolean = false, storeId: Long = this.storeId) =
        Cart(id = cartId, userId = userId, storeId = storeId, isOrdered = isOrdered, createdAt = 0L, updatedAt = 0L)

    private fun makeCartProduct() =
        CartProduct(id = 1L, cartId = cartId, productId = productId, quantity = 1, unitPrice = 8000)

    private fun makeRequest(qty: Int = 1) = AddCartItemRequest(productId = productId, quantity = qty)

    // --- addItem ---

    @Test
    fun `addItem - creates new cart and adds item when no cart exists`() {
        val newCart = makeCart()
        given(storeServiceClient.getProduct(productId)).willReturn(activeProduct)
        given(cartRepository.findByUserId(userId)).willReturn(null)
        given(cartRepository.save(any(Cart::class.java))).willReturn(newCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), principal)

        assertThat(result.storeId).isEqualTo(storeId)
        assertThat(result.items).hasSize(1)
        assertThat(result.totalPrice).isEqualTo(8000)
    }

    @Test
    fun `addItem - adds to existing cart with same store`() {
        val existingCart = makeCart()
        given(storeServiceClient.getProduct(productId)).willReturn(activeProduct)
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), principal)

        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `addItem - replaces cart when product is from a different store`() {
        val existingCart = makeCart(storeId = 999L)
        val resetCart = makeCart()
        given(storeServiceClient.getProduct(productId)).willReturn(activeProduct)
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartRepository.save(any(Cart::class.java))).willReturn(resetCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        cartService.addItem(makeRequest(), principal)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `addItem - resets checked-out cart when adding new items`() {
        val orderedCart = makeCart(isOrdered = true)
        val resetCart = makeCart()
        given(storeServiceClient.getProduct(productId)).willReturn(activeProduct)
        given(cartRepository.findByUserId(userId)).willReturn(orderedCart)
        given(cartRepository.save(any(Cart::class.java))).willReturn(resetCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        cartService.addItem(makeRequest(), principal)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `addItem - increments quantity when item already in cart`() {
        val existingItem = makeCartProduct()
        val existingCart = makeCart()
        given(storeServiceClient.getProduct(productId)).willReturn(activeProduct)
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(existingItem)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(existingItem)
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(existingItem))

        cartService.addItem(makeRequest(qty = 2), principal)

        assertThat(existingItem.quantity).isEqualTo(3)
    }

    @Test
    fun `addItem - product not available throws IllegalArgumentException`() {
        given(storeServiceClient.getProduct(productId)).willReturn(inactiveProduct)

        assertThatThrownBy { cartService.addItem(makeRequest(), principal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Product is not available")
    }

    // --- getMyCart ---

    @Test
    fun `getMyCart - returns cart response`() {
        given(cartRepository.findByUserId(userId)).willReturn(makeCart())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.getMyCart(principal)

        assertThat(result.id).isEqualTo(cartId)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `getMyCart - no cart throws IllegalArgumentException`() {
        given(cartRepository.findByUserId(userId)).willReturn(null)

        assertThatThrownBy { cartService.getMyCart(principal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cart not found")
    }

    // --- removeItem ---

    @Test
    fun `removeItem - happy path deletes item`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartRepository.save(any(Cart::class.java))).willReturn(makeCart())

        cartService.removeItem(cartId, productId, principal)

        then(cartProductRepository).should().deleteByCartIdAndProductId(cartId, productId)
    }

    @Test
    fun `removeItem - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.removeItem(cartId, productId, UserPrincipal(99L, "other@example.com", UserRole.CUSTOMER)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    // --- clearCart ---

    @Test
    fun `clearCart - happy path deletes all items`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartRepository.save(any(Cart::class.java))).willReturn(makeCart())

        cartService.clearCart(cartId, principal)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `clearCart - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.clearCart(cartId, UserPrincipal(99L, "other@example.com", UserRole.CUSTOMER)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    // --- checkout ---

    @Test
    fun `checkout - happy path creates order and marks cart as ordered`() {
        val cart = makeCart()
        val items = listOf(makeCartProduct())
        val order = Order(1L, cartId, userId, storeId, 8000, OrderStatus.PENDING, 0L, 0L)
        given(cartRepository.findById(cartId)).willReturn(Optional.of(cart))
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(items)
        given(orderRepository.save(any(Order::class.java))).willReturn(order)
        given(cartRepository.save(any(Cart::class.java))).willReturn(cart)

        val result = cartService.checkout(cartId, principal)

        assertThat(result.totalPrice).isEqualTo(8000)
        assertThat(result.status).isEqualTo("PENDING")
        assertThat(cart.isOrdered).isTrue()
    }

    @Test
    fun `checkout - already checked out throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart(isOrdered = true)))

        assertThatThrownBy { cartService.checkout(cartId, principal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Cart already checked out")
    }

    @Test
    fun `checkout - empty cart throws IllegalArgumentException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(emptyList())

        assertThatThrownBy { cartService.checkout(cartId, principal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cart is empty")
    }

    @Test
    fun `checkout - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.checkout(cartId, UserPrincipal(99L, "other@example.com", UserRole.CUSTOMER)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }
}
