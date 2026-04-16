package order.dto.cart

import order.entity.cart.Cart
import order.entity.cart.CartProduct

data class CartProductResponse(
    val id: Long,
    val productId: Long,
    val quantity: Long,
    val unitPrice: Long
) {
    companion object {
        fun of(cartProduct: CartProduct) = CartProductResponse(
            id        = cartProduct.id,
            productId = cartProduct.productId,
            quantity  = cartProduct.quantity,
            unitPrice = cartProduct.unitPrice
        )
    }
}

data class CartResponse(
    val id: Long,
    val storeId: Long,
    val isOrdered: Boolean,
    val items: List<CartProductResponse>,
    val totalPrice: Long
) {
    companion object {
        fun of(cart: Cart, items: List<CartProduct>) = CartResponse(
            id         = cart.id,
            storeId    = cart.storeId,
            isOrdered  = cart.isOrdered,
            items      = items.map { CartProductResponse.of(it) },
            totalPrice = items.sumOf { it.unitPrice * it.quantity }
        )
    }
}
