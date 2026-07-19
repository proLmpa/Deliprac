package ai.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class OrderItemInfo(val productId: Long, val productName: String, val quantity: Long, val unitPrice: Long)
data class OrderInfo(val id: Long, val storeId: Long, val status: String, val totalPrice: Long, val items: List<OrderItemInfo>)

@Component
class OrderClient(@Qualifier("orderRestClient") private val client: RestClient) {

    fun getUserOrders(token: String): List<OrderInfo> =
        client.post()
            .uri("/api/users/me/orders")
            .header("Authorization", token)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<OrderInfo>>() {})!!
}
