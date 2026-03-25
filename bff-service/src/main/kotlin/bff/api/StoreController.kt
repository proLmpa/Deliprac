package bff.api

import bff.client.StoreClient
import bff.dto.CreateProductRequest
import bff.dto.CreateReviewRequest
import bff.dto.CreateStoreRequest
import bff.dto.DeactivateProductRequest
import bff.dto.DeactivateStoreRequest
import bff.dto.DeleteReviewRequest
import bff.dto.FindProductRequest
import bff.dto.FindStoreRequest
import bff.dto.IncrementPopularityRequest
import bff.dto.ListProductRequest
import bff.dto.ListReviewRequest
import bff.dto.ListStoreRequest
import bff.dto.PopularProductRequest
import bff.dto.ProductResponse
import bff.dto.ReviewResponse
import bff.dto.StoreResponse
import bff.dto.UpdateProductRequest
import bff.dto.UpdateStoreRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreController(private val storeClient: StoreClient) {

    // ── Store ──────────────────────────────────────────────────────────────

    @PostMapping("/api/stores")
    fun createStore(@RequestBody request: CreateStoreRequest, httpRequest: HttpServletRequest): StoreResponse =
        storeClient.createStore(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/list")
    fun listStores(@RequestBody request: ListStoreRequest, httpRequest: HttpServletRequest): List<StoreResponse> =
        storeClient.listStores(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/mine")
    fun myStores(httpRequest: HttpServletRequest): List<StoreResponse> =
        storeClient.myStores(httpRequest.bearerToken())

    @PostMapping("/api/stores/find")
    fun findStore(@RequestBody request: FindStoreRequest, httpRequest: HttpServletRequest): StoreResponse =
        storeClient.findStore(request, httpRequest.bearerToken())

    @PutMapping("/api/stores")
    fun updateStore(@RequestBody request: UpdateStoreRequest, httpRequest: HttpServletRequest): StoreResponse =
        storeClient.updateStore(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/deactivate")
    fun deactivateStore(@RequestBody request: DeactivateStoreRequest, httpRequest: HttpServletRequest) =
        storeClient.deactivateStore(request, httpRequest.bearerToken())

    // ── Product ────────────────────────────────────────────────────────────

    @PostMapping("/api/stores/products")
    fun createProduct(@RequestBody request: CreateProductRequest, httpRequest: HttpServletRequest): ProductResponse =
        storeClient.createProduct(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/products/list")
    fun listProducts(@RequestBody request: ListProductRequest, httpRequest: HttpServletRequest): List<ProductResponse> =
        storeClient.listProducts(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/products/find")
    fun findProduct(@RequestBody request: FindProductRequest, httpRequest: HttpServletRequest): ProductResponse =
        storeClient.findProduct(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/products")
    fun updateProduct(@RequestBody request: UpdateProductRequest, httpRequest: HttpServletRequest): ProductResponse =
        storeClient.updateProduct(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/products/deactivate")
    fun deactivateProduct(@RequestBody request: DeactivateProductRequest, httpRequest: HttpServletRequest) =
        storeClient.deactivateProduct(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/products/popularity")
    fun incrementPopularity(@RequestBody request: IncrementPopularityRequest, httpRequest: HttpServletRequest): ProductResponse =
        storeClient.incrementPopularity(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/statistics/popular-products")
    fun popularProducts(@RequestBody request: PopularProductRequest, httpRequest: HttpServletRequest): List<ProductResponse> =
        storeClient.popularProducts(request, httpRequest.bearerToken())

    // ── Review ─────────────────────────────────────────────────────────────

    @PostMapping("/api/stores/reviews")
    fun createReview(@RequestBody request: CreateReviewRequest, httpRequest: HttpServletRequest): ReviewResponse =
        storeClient.createReview(request, httpRequest.bearerToken())

    @PostMapping("/api/stores/reviews/list")
    fun listReviews(@RequestBody request: ListReviewRequest, httpRequest: HttpServletRequest): List<ReviewResponse> =
        storeClient.listReviews(request, httpRequest.bearerToken())

    @DeleteMapping("/api/stores/reviews")
    fun deleteReview(@RequestBody request: DeleteReviewRequest, httpRequest: HttpServletRequest) =
        storeClient.deleteReview(request, httpRequest.bearerToken())
}
