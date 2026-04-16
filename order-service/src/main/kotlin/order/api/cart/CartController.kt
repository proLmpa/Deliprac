package order.api.cart

import common.security.currentUser
import order.dto.cart.AddCartItemRequest
import order.dto.cart.CartResponse
import order.dto.cart.CheckoutRequest
import order.dto.cart.ClearCartRequest
import order.dto.cart.RemoveCartItemRequest
import order.dto.order.OrderResponse
import order.service.cart.CartService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CartController(private val cartService: CartService) {

    @PostMapping("/api/carts")
    fun addItem(@RequestBody request: AddCartItemRequest): CartResponse =
        cartService.addItem(request, currentUser().id)

    @PostMapping("/api/carts/me")
    fun getMyCart(): CartResponse =
        cartService.getMyCart(currentUser().id)

    @DeleteMapping("/api/carts/products")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(@RequestBody request: RemoveCartItemRequest) {
        cartService.removeItem(request.cartId, request.productId, currentUser().id)
    }

    @DeleteMapping("/api/carts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearCart(@RequestBody request: ClearCartRequest) {
        cartService.clearCart(request.cartId, currentUser().id)
    }

    @PutMapping("/api/carts/checkout")
    fun checkout(@RequestBody request: CheckoutRequest): OrderResponse =
        cartService.checkout(request.cartId, currentUser().id)
}
