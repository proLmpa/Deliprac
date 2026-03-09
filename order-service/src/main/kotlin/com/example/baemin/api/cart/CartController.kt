package com.example.baemin.api.cart

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.cart.AddCartItemRequest
import com.example.baemin.dto.cart.CartResponse
import com.example.baemin.dto.order.OrderResponse
import com.example.baemin.service.cart.CartService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
        return cartService.addItem(request, currentUser())
    }

    @GetMapping("/api/carts")
    fun getMyCart(): CartResponse {
        return cartService.getMyCart(currentUser())
    }

    @DeleteMapping("/api/carts/{cartId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(
        @PathVariable cartId: Long,
        @PathVariable productId: Long
    ) {
        cartService.removeItem(cartId, productId, currentUser())
    }

    @DeleteMapping("/api/carts/{cartId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearCart(@PathVariable cartId: Long) {
        cartService.clearCart(cartId, currentUser())
    }

    @PutMapping("/api/carts/{cartId}/checkout")
    fun checkout(@PathVariable cartId: Long): OrderResponse {
        return cartService.checkout(cartId, currentUser())
    }
}
