package order.api.order

import order.dto.order.FindOrderRequest
import order.dto.order.OrderResponse
import order.service.order.OrderService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Internal endpoints for service-to-service communication.
 * Not exposed to external clients — no auth required.
 */
@RestController
class InternalOrderController(private val orderService: OrderService) {

    @PostMapping("/internal/orders/find")
    fun getOrder(@RequestBody request: FindOrderRequest): OrderResponse {
        val order = orderService.getById(request.orderId)
        return OrderResponse.of(order)
    }
}
