package com.example.baemin.api.product

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.product.ProductResponse
import com.example.baemin.service.product.ProductStatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreStatisticsController(private val productStatisticsService: ProductStatisticsService) {

    @GetMapping("/api/stores/{storeId}/statistics/popular-products")
    fun getPopularProducts(@PathVariable storeId: Long): List<ProductResponse> {
        return productStatisticsService.getPopularProducts(storeId, currentUser()).map { ProductResponse.of(it) }
    }
}
