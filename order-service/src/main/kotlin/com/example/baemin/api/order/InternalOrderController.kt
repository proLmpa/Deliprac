package com.example.baemin.api.order

import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.service.order.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * Internal endpoints for service-to-service communication.
 * Not exposed to external clients — no auth required.
 */
@RestController
class InternalOrderController(private val orderService: OrderService) {

    @GetMapping("/internal/orders/{orderId}")
    fun getOrder(@PathVariable orderId: Long): OrderResponse {
        val order = orderService.getById(orderId)
        return OrderResponse.of(order)
    }
}
