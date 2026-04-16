package order.api.order

import common.security.currentUser
import order.dto.order.OrderResponse
import order.service.order.OrderService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserOrderController(private val orderService: OrderService) {

    @PostMapping("/api/users/me/orders")
    fun listMyOrders(): List<OrderResponse> =
        orderService.listByUser(currentUser().id)
}
