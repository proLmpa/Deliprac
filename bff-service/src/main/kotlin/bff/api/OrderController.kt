package bff.api

import bff.client.AddCartItemRequest
import bff.client.CreateNotificationRequest
import bff.client.NotificationClient
import bff.client.NotificationItemData
import bff.client.OrderClient
import bff.client.StoreClient
import bff.dto.AddToCartRequest
import bff.dto.CartResponse
import bff.dto.CheckoutRequest
import bff.dto.ClearCartRequest
import bff.dto.FindProductRequest
import bff.dto.FindStoreRequest
import bff.dto.ListOrderRequest
import bff.dto.ListProductRequest
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
    private val storeClient: StoreClient,
    private val notificationClient: NotificationClient
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
    fun getMyCart(httpRequest: HttpServletRequest): CartResponse? =
        orderClient.getMyCart(httpRequest.bearerToken())

    @DeleteMapping("/api/carts/products")
    fun removeCartItem(@RequestBody request: RemoveCartItemRequest, httpRequest: HttpServletRequest) =
        orderClient.removeCartItem(request, httpRequest.bearerToken())

    @DeleteMapping("/api/carts")
    fun clearCart(@RequestBody request: ClearCartRequest, httpRequest: HttpServletRequest) =
        orderClient.clearCart(request, httpRequest.bearerToken())

    @PutMapping("/api/carts/checkout")
    fun checkout(@RequestBody request: CheckoutRequest, httpRequest: HttpServletRequest): OrderResponse {
        val token = httpRequest.bearerToken()
        val cart  = orderClient.getMyCart(token)
        val order = orderClient.checkout(request, token)
        val store = storeClient.findStore(FindStoreRequest(order.storeId), token)
        val productNames = cart?.let {
            storeClient.listProducts(ListProductRequest(it.storeId), token)
                .associate { p -> p.id to p.name }
        } ?: emptyMap()
        val items = cart?.items?.map { item ->
            NotificationItemData(
                productName = productNames[item.productId] ?: "상품 #${item.productId}",
                unitPrice   = item.unitPrice,
                quantity    = item.quantity
            )
        } ?: emptyList()
        notificationClient.createNotification(
            CreateNotificationRequest(
                recipientId = store.userId,
                type        = "NEW_ORDER",
                title       = "새 주문 접수",
                content     = "새 주문이 접수되었습니다.",
                storeId     = order.storeId,
                storeName   = store.name,
                expiry      = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                items       = items
            )
        )
        return order
    }

    // ── Order ──────────────────────────────────────────────────────────────

    @PostMapping("/api/stores/orders/list")
    fun listStoreOrders(@RequestBody request: ListOrderRequest, httpRequest: HttpServletRequest): List<OrderResponse> =
        orderClient.listStoreOrders(request, httpRequest.bearerToken())

    @PutMapping("/api/stores/orders/sold")
    fun markSold(@RequestBody request: MarkOrderRequest, httpRequest: HttpServletRequest): OrderResponse {
        val token = httpRequest.bearerToken()
        val order = orderClient.markSold(request, token)
        val store = storeClient.findStore(FindStoreRequest(order.storeId), token)
        val productNames = storeClient.listProducts(ListProductRequest(order.storeId), token)
            .associate { it.id to it.name }
        val items = order.items.map { item ->
            NotificationItemData(
                productName = productNames[item.productId] ?: "상품 #${item.productId}",
                unitPrice   = item.unitPrice,
                quantity    = item.quantity
            )
        }
        notificationClient.createNotification(
            CreateNotificationRequest(
                recipientId = order.userId,
                type        = "ORDER_SOLD",
                title       = "주문 완료",
                content     = "주문이 완료되었습니다.",
                storeId     = order.storeId,
                storeName   = store.name,
                expiry      = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                items       = items
            )
        )
        return order
    }

    @PutMapping("/api/stores/orders/cancel")
    fun markCanceled(@RequestBody request: MarkOrderRequest, httpRequest: HttpServletRequest): OrderResponse {
        val token = httpRequest.bearerToken()
        val order = orderClient.markCanceled(request, token)
        val store = storeClient.findStore(FindStoreRequest(order.storeId), token)
        val productNames = storeClient.listProducts(ListProductRequest(order.storeId), token)
            .associate { it.id to it.name }
        val items = order.items.map { item ->
            NotificationItemData(
                productName = productNames[item.productId] ?: "상품 #${item.productId}",
                unitPrice   = item.unitPrice,
                quantity    = item.quantity
            )
        }
        notificationClient.createNotification(
            CreateNotificationRequest(
                recipientId = order.userId,
                type        = "ORDER_CANCELED",
                title       = "주문 취소",
                content     = "주문이 취소되었습니다.",
                storeId     = order.storeId,
                storeName   = store.name,
                expiry      = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                items       = items
            )
        )
        return order
    }

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
