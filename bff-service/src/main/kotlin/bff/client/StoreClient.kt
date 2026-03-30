package bff.client

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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class StoreClient(@Qualifier("storeRestClient") private val client: RestClient) {

    // ── Store ──────────────────────────────────────────────────────────────

    fun createStore(request: CreateStoreRequest, token: String): StoreResponse =
        client.post()
            .uri("/api/stores")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(StoreResponse::class.java)!!

    fun listStores(request: ListStoreRequest, token: String): List<StoreResponse> =
        client.post()
            .uri("/api/stores/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<StoreResponse>>() {})!!

    fun myStores(token: String): List<StoreResponse> =
        client.post()
            .uri("/api/stores/mine")
            .header("Authorization", token)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<StoreResponse>>() {})!!

    fun findStore(request: FindStoreRequest, token: String): StoreResponse =
        client.post()
            .uri("/api/stores/find")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(StoreResponse::class.java)!!

    fun updateStore(request: UpdateStoreRequest, token: String): StoreResponse =
        client.put()
            .uri("/api/stores")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(StoreResponse::class.java)!!

    fun deactivateStore(request: DeactivateStoreRequest, token: String): Unit =
        client.put()
            .uri("/api/stores/deactivate")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    // ── Product ────────────────────────────────────────────────────────────

    fun createProduct(request: CreateProductRequest, token: String): ProductResponse =
        client.post()
            .uri("/api/stores/products")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(ProductResponse::class.java)!!

    fun listProducts(request: ListProductRequest, token: String): List<ProductResponse> =
        client.post()
            .uri("/api/stores/products/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<ProductResponse>>() {})!!

    fun findProduct(request: FindProductRequest, token: String): ProductResponse =
        client.post()
            .uri("/api/stores/products/find")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(ProductResponse::class.java)!!

    fun updateProduct(request: UpdateProductRequest, token: String): ProductResponse =
        client.put()
            .uri("/api/stores/products")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(ProductResponse::class.java)!!

    fun deactivateProduct(request: DeactivateProductRequest, token: String): Unit =
        client.put()
            .uri("/api/stores/products/deactivate")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    fun incrementPopularity(request: IncrementPopularityRequest, token: String): ProductResponse =
        client.put()
            .uri("/api/stores/products/popularity")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(ProductResponse::class.java)!!

    fun popularProducts(request: PopularProductRequest, token: String): List<ProductResponse> =
        client.post()
            .uri("/api/stores/statistics/popular-products")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<ProductResponse>>() {})!!

    // ── Review ─────────────────────────────────────────────────────────────

    fun createReview(request: CreateReviewRequest, token: String): ReviewResponse =
        client.post()
            .uri("/api/stores/reviews")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(ReviewResponse::class.java)!!

    fun listReviews(request: ListReviewRequest, token: String): List<ReviewResponse> =
        client.post()
            .uri("/api/stores/reviews/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<ReviewResponse>>() {})!!

    fun deleteReview(request: DeleteReviewRequest, token: String): Unit =
        client.method(HttpMethod.DELETE)
            .uri("/api/stores/reviews")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}
}
