package order.service.order

import common.security.UserRole
import order.entity.cart.CartProduct
import order.entity.order.Order
import order.entity.order.OrderStatus
import order.repository.cart.CartProductRepository
import order.repository.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock private lateinit var orderRepository: OrderRepository
    @Mock private lateinit var cartProductRepository: CartProductRepository
    @InjectMocks private lateinit var orderService: OrderService

    private val ownerId    = 1L
    private val customerId = 2L
    private val storeId    = 10L
    private val cartId     = 50L
    private val orderId    = 200L
    private val productId  = 100L

    private fun makeOrder(status: OrderStatus = OrderStatus.PENDING, storeId: Long = this.storeId) =
        Order(orderId, cartId, customerId, storeId, 8000, status, 0L, 0L)

    private fun makeCartProduct() =
        CartProduct(1L, cartId, productId, 2, 8000)

    // --- listByStore ---

    @Test
    fun `listByStore - happy path returns list`() {
        given(orderRepository.findAllByStoreId(storeId)).willReturn(listOf(makeOrder()))

        val result = orderService.listByStore(storeId, UserRole.OWNER)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `listByStore - non-OWNER throws IllegalStateException`() {
        assertThatThrownBy { orderService.listByStore(storeId, UserRole.CUSTOMER) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can view store orders")
    }

    // --- markSold ---

    @Test
    fun `markSold - happy path sets status SOLD`() {
        val order = makeOrder()
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order))
        given(orderRepository.save(any(Order::class.java))).willReturn(order)

        val result = orderService.markSold(storeId, orderId, UserRole.OWNER)

        assertThat(result.status).isEqualTo(OrderStatus.SOLD)
        assertThat(order.status).isEqualTo(OrderStatus.SOLD)
    }

    @Test
    fun `markSold - order not found throws IllegalArgumentException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.empty())

        assertThatThrownBy { orderService.markSold(storeId, orderId, UserRole.OWNER) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order not found")
    }

    @Test
    fun `markSold - order belongs to different store throws IllegalArgumentException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(storeId = 999L)))

        assertThatThrownBy { orderService.markSold(storeId, orderId, UserRole.OWNER) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Order not found in this store")
    }

    @Test
    fun `markSold - non-PENDING status throws IllegalStateException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(status = OrderStatus.CANCELED)))

        assertThatThrownBy { orderService.markSold(storeId, orderId, UserRole.OWNER) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Order cannot be marked as sold")
    }

    // --- markCanceled ---

    @Test
    fun `markCanceled - happy path sets status CANCELED`() {
        val order = makeOrder()
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order))
        given(orderRepository.save(any(Order::class.java))).willReturn(order)

        val result = orderService.markCanceled(storeId, orderId, UserRole.OWNER)

        assertThat(result.status).isEqualTo(OrderStatus.CANCELED)
        assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
    }

    @Test
    fun `markCanceled - non-PENDING status throws IllegalStateException`() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(makeOrder(status = OrderStatus.SOLD)))

        assertThatThrownBy { orderService.markCanceled(storeId, orderId, UserRole.OWNER) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Order cannot be canceled")
    }

    // --- listByUser ---

    @Test
    fun `listByUser - returns user orders`() {
        given(orderRepository.findAllByUserId(customerId)).willReturn(listOf(makeOrder()))

        val result = orderService.listByUser(customerId)

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
    private val utc = ZoneId.of("UTC")

    @Test
    fun `getRevenue - happy path returns total revenue`() {
        given(orderRepository.sumRevenueByStoreAndMonth(storeId, 2026, 3, utc)).willReturn(50000)

        val result = statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER)

        assertThat(result).isEqualTo(50000)
    }

    @Test
    fun `getRevenue - non-OWNER throws IllegalStateException`() {
        assertThatThrownBy { statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.CUSTOMER) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can view revenue statistics")
    }

    @Test
    fun `getRevenue - returns zero when no sold orders`() {
        given(orderRepository.sumRevenueByStoreAndMonth(storeId, 2026, 3, utc)).willReturn(0)

        val result = statisticsService.getRevenue(storeId, 2026, 3, utc, UserRole.OWNER)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `getSpending - returns total spending for user`() {
        given(orderRepository.sumSpendingByUserAndMonth(customerId, 2026, 3, utc)).willReturn(24000)

        val result = statisticsService.getSpending(2026, 3, utc, customerId)

        assertThat(result).isEqualTo(24000)
    }

    @Test
    fun `getSpending - returns zero when no spending`() {
        given(orderRepository.sumSpendingByUserAndMonth(customerId, 2026, 3, utc)).willReturn(0)

        val result = statisticsService.getSpending(2026, 3, utc, customerId)

        assertThat(result).isEqualTo(0)
    }
}
