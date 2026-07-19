package ai.tools

import ai.client.OrderInfo
import ai.client.ProductInfo
import ai.client.StoreClient
import ai.client.StoreInfo
import ai.config.AiRequestContext
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

@Component
class BaeminTools(
    private val storeClient: StoreClient,
    private val aiRequestContext: AiRequestContext
) {

    private val token: String
        get() = aiRequestContext.jwt ?: error("JWT not set in AiRequestContext")

    @Tool(description = "List all available stores. sortBy must be one of: RATING, LATEST, POPULARITY")
    fun listStores(sortBy: String): List<StoreInfo> =
        storeClient.listStores(sortBy, token)

    @Tool(description = "Get detailed information about a specific store by its ID")
    fun getStoreDetails(storeId: Long): StoreInfo =
        storeClient.findStore(storeId, token)

    @Tool(description = "List all products (menu items) for a specific store by its ID")
    fun getProducts(storeId: Long): List<ProductInfo> =
        storeClient.listProducts(storeId, token)
}
