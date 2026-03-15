package store.api.product

import store.dto.product.FindInternalProductRequest
import store.dto.product.ProductResponse
import store.service.product.ProductService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Internal endpoints for service-to-service communication.
 * Not exposed to external clients — no auth required.
 */
@RestController
class InternalProductController(private val productService: ProductService) {

    @PostMapping("/internal/products/find")
    fun getProduct(@RequestBody request: FindInternalProductRequest): ProductResponse {
        return ProductResponse.of(productService.getById(request.productId))
    }

    @PutMapping("/internal/products/{productId}/popularity")
    fun incrementPopularity(
        @PathVariable productId: Long,
        @RequestParam delta: Long
    ) {
        productService.incrementPopularity(productId, delta)
    }
}
