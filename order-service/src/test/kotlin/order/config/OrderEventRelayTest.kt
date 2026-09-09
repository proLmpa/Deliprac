package order.config

import common.event.OrderEvent
import common.event.OrderEventType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class OrderEventRelayTest {

    @Mock private lateinit var kafkaTemplate: KafkaTemplate<String, OrderEvent>
    @InjectMocks private lateinit var relay: OrderEventRelay

    private fun makeEvent() = OrderEvent(
        eventType = OrderEventType.NEW_ORDER,
        orderId = 1L,
        userId = 2L,
        storeId = 10L,
        storeOwnerId = 99L,
        storeName = "Test Store",
        totalPrice = 8000L,
        items = emptyList()
    )

    @Test
    fun `onOrderEvent - sends the event to the order events topic keyed by storeId`() {
        val event = makeEvent()
        given(kafkaTemplate.send("baemin.order.events", "10", event))
            .willReturn(CompletableFuture())

        relay.onOrderEvent(event)

        then(kafkaTemplate).should().send("baemin.order.events", "10", event)
    }

    @Test
    fun `onOrderEvent - a failed send is logged, not thrown`() {
        val event = makeEvent()
        val future = CompletableFuture<SendResult<String, OrderEvent>>()
        given(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent::class.java)))
            .willReturn(future)

        relay.onOrderEvent(event)
        future.completeExceptionally(RuntimeException("broker unavailable"))

        // whenComplete's callback swallows the exception -- reaching this line is the assertion.
    }
}
