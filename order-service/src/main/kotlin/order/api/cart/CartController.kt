package order.api.cart

import common.security.currentUser
import order.dto.cart.AddCartItemRequest
import order.dto.cart.CartResponse
import order.dto.order.OrderResponse
import order.service.cart.CartService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CartController(private val cartService: CartService) {

    @PostMapping("/api/carts")
    fun addItem(@RequestBody request: AddCartItemRequest): CartResponse {
        val cartInfo = cartService.addItem(request, currentUser().id)
        return CartResponse.of(cartInfo.cart, cartInfo.items)
    }

    @PostMapping("/api/carts/me")
    fun getMyCart(): CartResponse {
        val cartInfo = cartService.getMyCart(currentUser().id)
        return CartResponse.of(cartInfo.cart, cartInfo.items)
    }

    @DeleteMapping("/api/carts/{cartId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(
        @PathVariable cartId: Long,
        @PathVariable productId: Long
    ) {
        cartService.removeItem(cartId, productId, currentUser().id)
    }

    @DeleteMapping("/api/carts/{cartId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearCart(@PathVariable cartId: Long) {
        cartService.clearCart(cartId, currentUser().id)
    }

    @PutMapping("/api/carts/{cartId}/checkout")
    fun checkout(@PathVariable cartId: Long): OrderResponse {
        val order = cartService.checkout(cartId, currentUser().id)
        return OrderResponse.of(order)
    }
}
