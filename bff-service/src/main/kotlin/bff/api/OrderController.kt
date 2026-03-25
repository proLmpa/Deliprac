package bff.api

import bff.client.AddCartItemRequest
import bff.client.OrderClient
import bff.client.StoreClient
import bff.dto.AddToCartRequest
import bff.dto.CartResponse
import bff.dto.CheckoutRequest
import bff.dto.ClearCartRequest
import bff.dto.FindProductRequest
import bff.dto.ListOrderRequest
import bff.dto.MarkOrderRequest
import bff.dto.OrderResponse
import bff.dto.RemoveCartItemRequest
import bff.dto.RevenueRequest
import bff.dto.RevenueResponse
import bff.dto.SpendingRequest
import bff.dto.SpendingResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderClient: OrderClient,
    private val storeClient: StoreClient
) {

    // ── Cart ───────────────────────────────────────────────────────────────

    @PostMapping("/api/carts")
    fun addToCart(@RequestBody request: AddToCartRequest, httpRequest: HttpServletRequest): CartResponse {
        val token = httpRequest.bearerToken()
        val product = storeClient.findProduct(FindProductRequest(request.storeId, request.productId), token)
        return orderClient.addCartItem(
            AddCartItemRequest(
                productId = request.productId,
                storeId = request.storeId,
                unitPrice = product.price,
                quantity = request.quantity
            ),
            token
        )
    }

    @PostMapping("/api/carts/me")
    fun getMyCart(httpRequest: HttpServletRequest): CartResponse =
        orderClient.getMyCart(httpRequest.bearerToken())

    @DeleteMapping("/api/carts/products")
    fun removeCartItem(@RequestBody request: RemoveCartItemRequest, httpRequest: HttpServletRequest): CartResponse =
        orderClient.removeCartItem(request, httpRequest.bearerToken())

    @DeleteMapping("/api/carts")
    fun clearCart(@RequestBody request: ClearCartRequest, httpRequest: HttpServletRequest) =
        orderClient.clearCart(request, httpRequest.bearerToken())

    @PutMapping("/api/carts/checkout")
    fun checkout(@RequestBody request: CheckoutRequest, httpRequest: HttpServletRequest): OrderResponse =
        orderClient.checkout(request, httpRequest.bearerToken())

    // ── Order ──────────────────────────────────────────────────────────────

    @PostMapping("/api/stores/orders/list")
    fun listStoreOrders(@RequestBody request: ListOrderRequest, httpRequest: HttpServletRequest): List<OrderResponse> =
        orderClient.listStoreOrders(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/orders/sold")
    fun markSold(@RequestBody request: MarkOrderRequest, httpRequest: HttpServletRequest): OrderResponse =
        orderClient.markSold(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/orders/cancel")
    fun markCanceled(@RequestBody request: MarkOrderRequest, httpRequest: HttpServletRequest): OrderResponse =
        orderClient.markCanceled(request, httpRequest.bearerToken())

    @PostMapping("/api/users/me/orders")
    fun myOrders(httpRequest: HttpServletRequest): List<OrderResponse> =
        orderClient.myOrders(httpRequest.bearerToken())

    // ── Statistics ─────────────────────────────────────────────────────────

    @PostMapping("/api/stores/statistics/revenue")
    fun storeRevenue(@RequestBody request: RevenueRequest, httpRequest: HttpServletRequest): RevenueResponse =
        orderClient.storeRevenue(request, httpRequest.bearerToken())

    @PostMapping("/api/users/me/statistics/spending")
    fun mySpending(@RequestBody request: SpendingRequest, httpRequest: HttpServletRequest): SpendingResponse =
        orderClient.mySpending(request, httpRequest.bearerToken())
}
