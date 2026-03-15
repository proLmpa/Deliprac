package store.api.product

import common.security.currentUser
import store.dto.product.CreateProductRequest
import store.dto.product.ProductResponse
import store.dto.product.UpdateProductRequest
import store.service.product.ProductService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(private val productService: ProductService) {

    @PostMapping("/api/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable storeId: Long,
        @RequestBody request: CreateProductRequest
    ): ProductResponse {
        val product = productService.create(storeId, request, currentUser())
        return ProductResponse.of(product)
    }

    @GetMapping("/api/stores/{storeId}/products")
    fun listByStore(@PathVariable storeId: Long): List<ProductResponse> {
        return productService.listByStore(storeId).map { ProductResponse.of(it) }
    }

    @GetMapping("/api/stores/{storeId}/products/{productId}")
    fun findById(
        @PathVariable storeId: Long,
        @PathVariable productId: Long
    ): ProductResponse {
        return ProductResponse.of(productService.findById(storeId, productId))
    }

    @PutMapping("/api/stores/{storeId}/products/{productId}")
    fun update(
        @PathVariable storeId: Long,
        @PathVariable productId: Long,
        @RequestBody request: UpdateProductRequest
    ): ProductResponse {
        val product = productService.update(storeId, productId, request, currentUser().id)
        return ProductResponse.of(product)
    }

    @PutMapping("/api/stores/{storeId}/products/{productId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(
        @PathVariable storeId: Long,
        @PathVariable productId: Long
    ) {
        productService.deactivate(storeId, productId, currentUser().id)
    }
}
