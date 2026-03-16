package store.api.product

import common.security.currentUser
import store.dto.product.CreateProductRequest
import store.dto.product.DeactivateProductRequest
import store.dto.product.FindProductRequest
import store.dto.product.IncrementPopularityRequest
import store.dto.product.ListProductRequest
import store.dto.product.ProductResponse
import store.dto.product.UpdateProductRequest
import store.service.product.ProductService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(private val productService: ProductService) {

    @PostMapping("/api/stores/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateProductRequest): ProductResponse {
        val product = productService.create(request.storeId, request, currentUser())
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

    @PutMapping("/api/stores/products")
    fun update(@RequestBody request: UpdateProductRequest): ProductResponse {
        val product = productService.update(request.storeId, request.productId, request, currentUser().id)
        return ProductResponse.of(product)
    }

    @PutMapping("/api/stores/products/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@RequestBody request: DeactivateProductRequest) {
        productService.deactivate(request.storeId, request.productId, currentUser().id)
    }

    @PutMapping("/api/stores/products/popularity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun incrementPopularity(@RequestBody request: IncrementPopularityRequest) {
        productService.incrementPopularity(request.storeId, request.productId, request.delta, currentUser().id)
    }
}
