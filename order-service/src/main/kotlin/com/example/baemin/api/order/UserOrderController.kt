package com.example.baemin.api.order

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.service.order.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserOrderController(private val orderService: OrderService) {

    @GetMapping("/api/users/me/orders")
    fun listMyOrders(): List<OrderResponse> {
        return orderService.listByUser(currentUser())
    }
}
