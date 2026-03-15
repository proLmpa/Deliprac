package order.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@JsonIgnoreProperties(ignoreUnknown = true)
data class RemoteProductInfo(
    val storeId: Long,
    val price: Long,
    val status: Boolean
)

private data class FindProductBody(val productId: Long)

@Component
class StoreServiceClient(
    @Value("\${store-service.url}") private val storeServiceUrl: String
) {
    private val restClient = RestClient.create()

    fun getProduct(productId: Long): RemoteProductInfo =
        restClient.post()
            .uri("$storeServiceUrl/internal/products/find")
            .contentType(MediaType.APPLICATION_JSON)
            .body(FindProductBody(productId))
            .retrieve()
            .body(RemoteProductInfo::class.java)!!

    fun incrementPopularity(productId: Long, delta: Long) {
        restClient.put()
            .uri("$storeServiceUrl/internal/products/$productId/popularity?delta=$delta")
            .retrieve()
            .toBodilessEntity()
    }
}
