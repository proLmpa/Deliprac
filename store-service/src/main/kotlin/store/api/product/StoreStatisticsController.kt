package store.api.product

import common.security.currentUser
import store.dto.product.PopularProductRequest
import store.dto.product.ProductResponse
import store.service.product.ProductStatisticsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreStatisticsController(private val productStatisticsService: ProductStatisticsService) {

    @PostMapping("/api/stores/statistics/popular-products")
    fun getPopularProducts(@RequestBody request: PopularProductRequest): List<ProductResponse> {
        return productStatisticsService.getPopularProducts(request.storeId, currentUser()).map { ProductResponse.of(it) }
    }
}
