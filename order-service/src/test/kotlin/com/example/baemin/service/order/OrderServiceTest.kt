package com.example.baemin.service.order

import com.example.baemin.client.StoreServiceClient
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.entity.cart.CartProduct
import com.example.baemin.entity.order.Order
import com.example.baemin.entity.order.OrderStatus
import com.example.baemin.repository.cart.CartProductRepository
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
class OrderServiceTest {

    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var cartProductRepository: CartProductRepository
    @Mock private lateinit var storeServiceClient: StoreServiceClient
    @InjectMocks private lateinit var orderService: OrderService

    private val ownerId    = 1L
    private val customerId = 2L
    private val storeId    = 10L
    private val cartId     = 50L
    private val orderId    = 200L
    private val productId  = 100L
    private val ownerPrincipal    = UserPrincipal(ownerId,    "owner@example.com",    UserRole.OWNER)
    private val customerPrincipal = UserPrincipal(customerId, "customer@example.com", UserRole.CUSTOMER)

    private fun makeOrder(status: OrderStatus = OrderStatus.PENDING, storeId: Long = this.storeId) =
        Order(orderId, cartId, customerId, storeId, 8000, status, 0L, 0L)

    private fun makeCartProduct() =
        CartProduct(1L, cartId, productId, 2, 8000)

    // --- listByStore ---

    @Test
    fun `listByStore - happy path returns list`() {
        given(orderRepository.findAllByStoreId(storeId)).willReturn(listOf(makeOrder()))

        val result = orderService.listByStore(storeId, ownerPrincipal)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `listByStore - non-OWNER throws IllegalStateException`() {
        assertThatThrownBy { orderService.listByStore(storeId, customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can view store orders")
    }

    // --- markSold ---

    @Test
    fun `markSold - happy path sets status SOLD and increments popularity`() {
        val order = makeOrder()
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order))
        given(orderRepository.save(any(Order::class.java))).willReturn(order)
        given(cartProductRepository.findAllByCartId(cartId)).willReturn(listOf(makeCartProduct()))

        val result = orderService.markSold(storeId, orderId, ownerPrincipal)

        assertThat(result.status).isEqualTo("SOLD")
        assertThat(order.status).isEqualTo(OrderStatus.SOLD)
        then(storeServiceClient).should().incrementPopularity(productId, 2)
    }

    @Test
    fun `markSold - order not found throws IllegalArgumentException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.empty())

        assertThatThrownBy { orderService.markSold(storeId, orderId, ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order not found")
    }

    @Test
    fun `markSold - order belongs to different store throws IllegalArgumentException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(storeId = 999L)))

        assertThatThrownBy { orderService.markSold(storeId, orderId, ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order not found in this store")
    }

    @Test
    fun `markSold - non-PENDING status throws IllegalStateException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(status = OrderStatus.CANCELED)))

        assertThatThrownBy { orderService.markSold(storeId, orderId, ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Order cannot be marked as sold")
    }

    // --- markCanceled ---

    @Test
    fun `markCanceled - happy path sets status CANCELED`() {
        val order = makeOrder()
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order))
        given(orderRepository.save(any(Order::class.java))).willReturn(order)

        val result = orderService.markCanceled(storeId, orderId, ownerPrincipal)

        assertThat(result.status).isEqualTo("CANCELED")
        assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
    }

    @Test
    fun `markCanceled - non-PENDING status throws IllegalStateException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(status = OrderStatus.SOLD)))

        assertThatThrownBy { orderService.markCanceled(storeId, orderId, ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Order cannot be canceled")
    }

    // --- listByUser ---

    @Test
    fun `listByUser - returns user orders`() {
        given(orderRepository.findAllByUserId(customerId)).willReturn(listOf(makeOrder()))

        val result = orderService.listByUser(customerPrincipal)

        assertThat(result).hasSize(1)
    }

    // --- getById ---

    @Test
    fun `getById - happy path returns order`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder()))

        val result = orderService.getById(orderId)

        assertThat(result.id).isEqualTo(orderId)
    }

    @Test
    fun `getById - not found throws IllegalArgumentException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.empty())

        assertThatThrownBy { orderService.getById(orderId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order not found")
    }
}

@ExtendWith(MockitoExtension::class)
class StatisticsServiceTest {

    @Mock private lateinit var orderRepository: OrderRepository
    @InjectMocks private lateinit var statisticsService: StatisticsService

    private val ownerId    = 1L
    private val customerId = 2L
    private val storeId    = 10L
    private val ownerPrincipal    = UserPrincipal(ownerId,    "owner@example.com",    UserRole.OWNER)
    private val customerPrincipal = UserPrincipal(customerId, "customer@example.com", UserRole.CUSTOMER)

    @Test
    fun `getRevenue - happy path returns revenue response`() {
        given(orderRepository.sumRevenueByStoreAndMonth(storeId, 2026, 3)).willReturn(50000)

        val result = statisticsService.getRevenue(storeId, 2026, 3, ownerPrincipal)

        assertThat(result.totalRevenue).isEqualTo(50000)
        assertThat(result.storeId).isEqualTo(storeId)
    }

    @Test
    fun `getRevenue - non-OWNER throws IllegalStateException`() {
        assertThatThrownBy { statisticsService.getRevenue(storeId, 2026, 3, customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can view revenue statistics")
    }

    @Test
    fun `getRevenue - returns zero when no sold orders`() {
        given(orderRepository.sumRevenueByStoreAndMonth(storeId, 2026, 3)).willReturn(0)

        val result = statisticsService.getRevenue(storeId, 2026, 3, ownerPrincipal)

        assertThat(result.totalRevenue).isEqualTo(0)
    }

    @Test
    fun `getSpending - returns spending response for user`() {
        given(orderRepository.sumSpendingByUserAndMonth(customerId, 2026, 3)).willReturn(24000)

        val result = statisticsService.getSpending(2026, 3, customerPrincipal)

        assertThat(result.totalSpending).isEqualTo(24000)
        assertThat(result.year).isEqualTo(2026)
    }
}
