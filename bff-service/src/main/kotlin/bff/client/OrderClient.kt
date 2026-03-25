package bff.client

import bff.dto.CartResponse
import bff.dto.CheckoutRequest
import bff.dto.ClearCartRequest
import bff.dto.ListOrderRequest
import bff.dto.MarkOrderRequest
import bff.dto.OrderResponse
import bff.dto.RemoveCartItemRequest
import bff.dto.RevenueRequest
import bff.dto.RevenueResponse
import bff.dto.SpendingRequest
import bff.dto.SpendingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

// Internal DTO: mirrors order-service's AddCartItemRequest (includes unitPrice)
data class AddCartItemRequest(
    val productId: Long,
    val storeId: Long,
    val unitPrice: Long,
    val quantity: Long
)

@Component
class OrderClient(@Qualifier("orderClient") private val client: RestClient) {

    // ── Cart ───────────────────────────────────────────────────────────────

    fun addCartItem(request: AddCartItemRequest, token: String): CartResponse =
        client.post()
            .uri("/api/carts")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(CartResponse::class.java)!!

    fun getMyCart(token: String): CartResponse =
        client.post()
            .uri("/api/carts/me")
            .header("Authorization", token)
            .retrieve()
            .body(CartResponse::class.java)!!

    fun removeCartItem(request: RemoveCartItemRequest, token: String): CartResponse =
        client.method(HttpMethod.DELETE)
            .uri("/api/carts/products")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(CartResponse::class.java)!!

    fun clearCart(request: ClearCartRequest, token: String): Unit =
        client.method(HttpMethod.DELETE)
            .uri("/api/carts")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    fun checkout(request: CheckoutRequest, token: String): OrderResponse =
        client.put()
            .uri("/api/carts/checkout")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(OrderResponse::class.java)!!

    // ── Order ──────────────────────────────────────────────────────────────

    fun listStoreOrders(request: ListOrderRequest, token: String): List<OrderResponse> =
        client.post()
            .uri("/api/stores/orders/list")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<OrderResponse>>() {})!!

    fun markSold(request: MarkOrderRequest, token: String): OrderResponse =
        client.put()
            .uri("/api/stores/orders/sold")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(OrderResponse::class.java)!!

    fun markCanceled(request: MarkOrderRequest, token: String): OrderResponse =
        client.put()
            .uri("/api/stores/orders/cancel")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(OrderResponse::class.java)!!

    fun myOrders(token: String): List<OrderResponse> =
        client.post()
            .uri("/api/users/me/orders")
            .header("Authorization", token)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<OrderResponse>>() {})!!

    // ── Statistics ─────────────────────────────────────────────────────────

    fun storeRevenue(request: RevenueRequest, token: String): RevenueResponse =
        client.post()
            .uri("/api/stores/statistics/revenue")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(RevenueResponse::class.java)!!

    fun mySpending(request: SpendingRequest, token: String): SpendingResponse =
        client.post()
            .uri("/api/users/me/statistics/spending")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(SpendingResponse::class.java)!!
}
