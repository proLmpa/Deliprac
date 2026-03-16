package order.service.cart

import order.dto.cart.AddCartItemRequest
import order.entity.cart.Cart
import order.entity.cart.CartProduct
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.cart.CartRepository
import order.repository.order.OrderRepository
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
    @InjectMocks private lateinit var cartService: CartService

    private val userId    = 1L
    private val storeId   = 10L
    private val cartId    = 50L
    private val productId = 100L
    private val unitPrice = 8000L

    private fun makeCart(isOrdered: Boolean = false, storeId: Long = this.storeId) =
        Cart(id = cartId, userId = userId, storeId = storeId, isOrdered = isOrdered, createdAt = 0L, updatedAt = 0L)

    private fun makeCartProduct() =
        CartProduct(id = 1L, cartId = cartId, productId = productId, quantity = 1L, unitPrice = unitPrice)

    private fun makeRequest(qty: Long = 1L, storeId: Long = this.storeId) =
        AddCartItemRequest(productId = productId, storeId = storeId, unitPrice = unitPrice, quantity = qty)

    // --- addItem ---

    @Test
    fun `addItem - creates new cart and adds item when no cart exists`() {
        val newCart = makeCart()
        given(cartRepository.findByUserId(userId)).willReturn(null)
        given(cartRepository.save(any(Cart::class.java))).willReturn(newCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), userId)

        assertThat(result.cart.storeId).isEqualTo(storeId)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `addItem - adds to existing cart with same store`() {
        val existingCart = makeCart()
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), userId)

        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `addItem - replaces cart when product is from a different store`() {
        val existingCart = makeCart(storeId = 999L)
        val resetCart = makeCart()
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartRepository.save(any(Cart::class.java))).willReturn(resetCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        cartService.addItem(makeRequest(), userId)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `addItem - resets checked-out cart when adding new items`() {
        val orderedCart = makeCart(isOrdered = true)
        val resetCart = makeCart()
        given(cartRepository.findByUserId(userId)).willReturn(orderedCart)
        given(cartRepository.save(any(Cart::class.java))).willReturn(resetCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        cartService.addItem(makeRequest(), userId)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `addItem - increments quantity when item already in cart`() {
        val existingItem = makeCartProduct()
        val existingCart = makeCart()
        given(cartRepository.findByUserId(userId)).willReturn(existingCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(existingItem)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(existingItem)
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(existingItem))

        cartService.addItem(makeRequest(qty = 2L), userId)

        assertThat(existingItem.quantity).isEqualTo(3L)
    }

    // --- getMyCart ---

    @Test
    fun `getMyCart - returns cart info`() {
        given(cartRepository.findByUserId(userId)).willReturn(makeCart())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.getMyCart(userId)

        assertThat(result.cart.id).isEqualTo(cartId)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `getMyCart - no cart throws IllegalArgumentException`() {
        given(cartRepository.findByUserId(userId)).willReturn(null)

        assertThatThrownBy { cartService.getMyCart(userId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cart not found")
    }

    // --- removeItem ---

    @Test
    fun `removeItem - happy path deletes item`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        cartService.removeItem(cartId, productId, userId)

        then(cartProductRepository).should().deleteByCartIdAndProductId(cartId, productId)
    }

    @Test
    fun `removeItem - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.removeItem(cartId, productId, 99L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    // --- clearCart ---

    @Test
    fun `clearCart - happy path deletes all items`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartRepository.save(any(Cart::class.java))).willReturn(makeCart())

        cartService.clearCart(cartId, userId)

        then(cartProductRepository).should().deleteByCartId(cartId)
    }

    @Test
    fun `clearCart - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.clearCart(cartId, 99L) }
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

        val result = cartService.checkout(cartId, userId)

        assertThat(result.totalPrice).isEqualTo(8000L)
        assertThat(result.status).isEqualTo(OrderStatus.PENDING)
        assertThat(cart.isOrdered).isTrue()
    }

    @Test
    fun `checkout - already checked out throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart(isOrdered = true)))

        assertThatThrownBy { cartService.checkout(cartId, userId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Cart already checked out")
    }

    @Test
    fun `checkout - empty cart throws IllegalArgumentException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(emptyList())

        assertThatThrownBy { cartService.checkout(cartId, userId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cart is empty")
    }

    @Test
    fun `checkout - wrong user throws IllegalStateException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.checkout(cartId, 99L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }
}
