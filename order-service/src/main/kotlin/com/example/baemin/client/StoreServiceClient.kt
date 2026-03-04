package com.example.baemin.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@JsonIgnoreProperties(ignoreUnknown = true)
data class RemoteProductInfo(
    val storeId: Long,
    val price: Int,
    val status: Boolean
)

@Component
class StoreServiceClient(
    @Value("\${store-service.url}") private val storeServiceUrl: String
) {
    private val restClient = RestClient.create()

    fun getProduct(productId: Long): RemoteProductInfo =
        restClient.get()
            .uri("$storeServiceUrl/internal/products/$productId")
            .retrieve()
            .body(RemoteProductInfo::class.java)!!

    fun incrementPopularity(productId: Long, delta: Int) {
        restClient.put()
            .uri("$storeServiceUrl/internal/products/$productId/popularity?delta=$delta")
            .retrieve()
            .toBodilessEntity()
    }
}
