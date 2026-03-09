package com.example.baemin.api.order

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.service.order.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(private val orderService: OrderService) {

    @GetMapping("/api/stores/{storeId}/orders")
    fun listByStore(@PathVariable storeId: Long): List<OrderResponse> {
        return orderService.listByStore(storeId, currentUser())
    }

    @PutMapping("/api/stores/{storeId}/orders/{orderId}/sold")
    fun markSold(
        @PathVariable storeId: Long,
        @PathVariable orderId: Long
    ): OrderResponse {
        return orderService.markSold(storeId, orderId, currentUser())
    }

    @PutMapping("/api/stores/{storeId}/orders/{orderId}/cancel")
    fun markCanceled(
        @PathVariable storeId: Long,
        @PathVariable orderId: Long
    ): OrderResponse {
        return orderService.markCanceled(storeId, orderId, currentUser())
    }
}
