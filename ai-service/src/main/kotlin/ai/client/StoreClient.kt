package ai.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class StoreInfo(val id: Long, val name: String, val address: String, val phone: String)
data class ProductInfo(val id: Long, val storeId: Long, val name: String, val description: String, val price: Long, val status: Boolean)

@Component
class StoreClient(@Qualifier("storeRestClient") private val client: RestClient) {

    fun listStores(sortBy: String, token: String): List<StoreInfo> =
        client.post()
            .uri("/api/stores/list")
            .header("Authorization", token)
            .body(mapOf("sortBy" to sortBy))
            .retrieve()
            .body(object : ParameterizedTypeReference<List<StoreInfo>>() {})!!

    fun findStore(storeId: Long, token: String): StoreInfo =
        client.post()
            .uri("/api/stores/find")
            .header("Authorization", token)
            .body(mapOf("id" to storeId))
            .retrieve()
            .body(StoreInfo::class.java)!!

    fun listProducts(storeId: Long, token: String): List<ProductInfo> =
        client.post()
            .uri("/api/stores/products/list")
            .header("Authorization", token)
            .body(mapOf("storeId" to storeId))
            .retrieve()
            .body(object : ParameterizedTypeReference<List<ProductInfo>>() {})!!
}
