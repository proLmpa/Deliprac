package order.api.order

import common.security.currentUser
import order.dto.order.OrderResponse
import order.service.order.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(private val orderService: OrderService) {

    @GetMapping("/api/stores/{storeId}/orders")
    fun listByStore(@PathVariable storeId: Long): List<OrderResponse> {
        return orderService.listByStore(storeId, currentUser().role).map { OrderResponse.of(it) }
    }

    @PutMapping("/api/stores/{storeId}/orders/{orderId}/sold")
    fun markSold(
        @PathVariable storeId: Long,
        @PathVariable orderId: Long
    ): OrderResponse {
        val order = orderService.markSold(storeId, orderId, currentUser().role)
        return OrderResponse.of(order)
    }

    @PutMapping("/api/stores/{storeId}/orders/{orderId}/cancel")
    fun markCanceled(
        @PathVariable storeId: Long,
        @PathVariable orderId: Long
    ): OrderResponse {
        val order = orderService.markCanceled(storeId, orderId, currentUser().role)
        return OrderResponse.of(order)
    }
}
