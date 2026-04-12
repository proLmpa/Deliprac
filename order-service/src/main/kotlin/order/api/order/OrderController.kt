package order.api.order

import common.security.currentUser
import order.dto.order.ListOrderRequest
import order.dto.order.MarkOrderRequest
import order.dto.order.OrderResponse
import order.service.order.OrderService
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

    @PutMapping("/api/stores/orders/sold")
    fun markSold(@RequestBody request: MarkOrderRequest): OrderResponse =
        orderService.markSold(request.storeId, request.orderId, currentUser().role)

    @PutMapping("/api/stores/orders/cancel")
    fun markCanceled(@RequestBody request: MarkOrderRequest): OrderResponse =
        orderService.markCanceled(request.storeId, request.orderId, currentUser().role)
}
