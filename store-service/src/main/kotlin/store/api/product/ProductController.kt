package store.api.product

import common.security.currentUser
import store.dto.product.CreateProductRequest
import store.dto.product.FindProductRequest
import store.dto.product.ListProductRequest
import store.dto.product.ProductResponse
import store.dto.product.UpdateProductRequest
import store.service.product.ProductService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
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

    @PostMapping("/api/stores/products/list")
    fun listByStore(@RequestBody request: ListProductRequest): List<ProductResponse> {
        return productService.listByStore(request.storeId).map { ProductResponse.of(it) }
    }

    @PostMapping("/api/stores/products/find")
    fun findById(@RequestBody request: FindProductRequest): ProductResponse {
        return ProductResponse.of(productService.findById(request.storeId, request.productId))
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

    @PutMapping("/api/stores/{storeId}/products/{productId}/popularity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun incrementPopularity(
        @PathVariable storeId: Long,
        @PathVariable productId: Long,
        @RequestParam delta: Long
    ) {
        productService.incrementPopularity(productId, delta)
    }
}
