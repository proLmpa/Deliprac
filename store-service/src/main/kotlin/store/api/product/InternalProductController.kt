package store.api.product

import store.dto.product.ProductResponse
import store.service.product.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Internal endpoints for service-to-service communication.
 * Not exposed to external clients — no auth required.
 */
@RestController
class InternalProductController(private val productService: ProductService) {

    @GetMapping("/internal/products/{productId}")
    fun getProduct(@PathVariable productId: Long): ProductResponse {
        return ProductResponse.of(productService.getById(productId))
    }

    @PutMapping("/internal/products/{productId}/popularity")
    fun incrementPopularity(
        @PathVariable productId: Long,
        @RequestParam delta: Int
    ) {
        productService.incrementPopularity(productId, delta)
    }
}
