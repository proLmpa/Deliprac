package order.api.order

import common.security.currentUser
import order.dto.order.OrderResponse
import order.service.order.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserOrderController(private val orderService: OrderService) {

    @GetMapping("/api/users/me/orders")
    fun listMyOrders(): List<OrderResponse> {
        return orderService.listByUser(currentUser().id).map { OrderResponse.of(it) }
    }
}
