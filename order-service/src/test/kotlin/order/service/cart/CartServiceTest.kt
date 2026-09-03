package order.service.cart

import common.event.OrderEvent
import common.exception.ConflictException
import common.exception.ForbiddenException
import common.exception.NotFoundException
import common.security.UserPrincipal
import common.security.UserRole
import order.dto.cart.AddCartItemRequest
import order.dto.cart.CheckoutRequest
import order.entity.cart.Cart
import order.entity.cart.CartProduct
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.cart.CartRepository
import order.repository.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CartServiceTest {

    @Mock private lateinit var cartRepository: CartRepository
    @Mock private lateinit var cartProductRepository: CartProductRepository
    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var kafkaTemplate: KafkaTemplate<String, OrderEvent>
    @InjectMocks private lateinit var cartService: CartService

    private val userId       = 1L
    private val storeId      = 10L
    private val cartId       = 50L
    private val productId    = 100L
    private val unitPrice    = 8000L
    private val storeOwnerId = 99L
    private val storeName    = "Test Store"

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun makeCart(storeId: Long = this.storeId) =
        Cart(id = cartId, userId = userId, storeId = storeId, isOrdered = false)

    private fun makeCartProduct() =
        CartProduct(id = 1L, cartId = cartId, productId = productId, quantity = 1L, unitPrice = unitPrice)

    private fun makeRequest(qty: Long = 1L, storeId: Long = this.storeId) =
        AddCartItemRequest(productId = productId, storeId = storeId, unitPrice = unitPrice, quantity = qty)

    private fun makeCheckoutRequest() =
        CheckoutRequest(cartId = cartId, storeOwnerId = storeOwnerId, storeName = storeName, items = emptyList())

    // --- addItem ---

    @Test
    fun `addItem - creates new cart when no active cart exists`() {
        val newCart = makeCart()
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(null)
        given(cartRepository.save(any(Cart::class.java))).willReturn(newCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), userId)

        assertThat(result.storeId).isEqualTo(storeId)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `addItem - reuses active cart with same store`() {
        val activeCart = makeCart()
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(activeCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(null)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(makeCartProduct())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.addItem(makeRequest(), userId)

        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `addItem - resets cart when product is from a different store`() {
        val activeCart = makeCart(storeId = 999L)
        val resetCart = makeCart()
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(activeCart)
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
        val activeCart = makeCart()
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(activeCart)
        given(cartProductRepository.findByCartIdAndProductId(cartId, productId)).willReturn(existingItem)
        given(cartProductRepository.save(any(CartProduct::class.java))).willReturn(existingItem)
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(existingItem))

        cartService.addItem(makeRequest(qty = 2L), userId)

        assertThat(existingItem.quantity).isEqualTo(3L)
    }

    // --- getMyCart ---

    @Test
    fun `getMyCart - returns active cart info`() {
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(makeCart())
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = cartService.getMyCart(userId)

        assertThat(result.id).isEqualTo(cartId)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `getMyCart - no active cart throws NotFoundException`() {
        given(cartRepository.findFirstByUserIdAndIsOrderedFalse(userId)).willReturn(null)

        assertThatThrownBy { cartService.getMyCart(userId) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Not found")
    }

    // --- removeItem ---

    @Test
    fun `removeItem - happy path deletes item`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        cartService.removeItem(cartId, productId, userId)

        then(cartProductRepository).should().deleteByCartIdAndProductId(cartId, productId)
    }

    @Test
    fun `removeItem - wrong user throws ForbiddenException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.removeItem(cartId, productId, 99L) }
            .isInstanceOf(ForbiddenException::class.java)
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
    fun `clearCart - wrong user throws ForbiddenException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.clearCart(cartId, 99L) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }

    // --- checkout ---

    @Test
    fun `checkout - happy path creates order and marks cart as ordered`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            UserPrincipal(userId, "customer@example.com", UserRole.CUSTOMER), null
        )
        val cart  = makeCart()
        val items = listOf(makeCartProduct())
        val order = Order(1L, cartId, userId, storeId, 8000, storeOwnerId, storeName, OrderStatus.PENDING)
        given(cartRepository.findById(cartId)).willReturn(Optional.of(cart))
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(items)
        given(orderRepository.save(any(Order::class.java))).willReturn(order)
        given(cartRepository.save(any(Cart::class.java))).willReturn(cart)

        val result = cartService.checkout(makeCheckoutRequest(), userId)

        assertThat(result.totalPrice).isEqualTo(8000L)
        assertThat(result.status).isEqualTo("PENDING")
        assertThat(cart.isOrdered).isTrue()
    }

    @Test
    fun `checkout - already checked out throws ConflictException`() {
        val orderedCart = Cart(cartId, userId, storeId, isOrdered = true)
        given(cartRepository.findById(cartId)).willReturn(Optional.of(orderedCart))

        assertThatThrownBy { cartService.checkout(makeCheckoutRequest(), userId) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("Invalid operation")
    }

    @Test
    fun `checkout - empty cart throws ConflictException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(emptyList())

        assertThatThrownBy { cartService.checkout(makeCheckoutRequest(), userId) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessage("Invalid operation")
    }

    @Test
    fun `checkout - wrong user throws ForbiddenException`() {
        given(cartRepository.findById(cartId)).willReturn(Optional.of(makeCart()))

        assertThatThrownBy { cartService.checkout(makeCheckoutRequest(), 99L) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }
}
