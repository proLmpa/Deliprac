package order.api.order

import common.security.currentUser
import order.dto.order.ListOrderRequest
import order.dto.order.OrderResponse
import order.service.order.OrderService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(private val orderService: OrderService) {

    @PostMapping("/api/stores/orders/list")
    fun listByStore(@RequestBody request: ListOrderRequest): List<OrderResponse> {
        return orderService.listByStore(request.storeId, currentUser().role).map { OrderResponse.of(it) }
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
