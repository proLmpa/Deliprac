package ai.service

import ai.client.OrderClient
import ai.client.ProductInfo
import ai.client.StoreClient
import ai.config.AiRequestContext
import ai.dto.RecommendInfo
import ai.dto.RecommendRequest
import ai.dto.RecommendedItem
import tools.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger {}

private val SYSTEM_PROMPT = """
    You are a food recommendation assistant for Baemin, a Korean food delivery platform.
    Recommend items from the current store's menu based on the user's preferences.
    Respond ONLY with valid JSON (no markdown):
    {"recommendations":[{"productName":"<String>","reason":"<1 sentence max>"}]}
    Recommend 1 to 3 items.
""".trimIndent()

@Service
class RecommendationService(
    private val chatClient: ChatClient,
    private val storeClient: StoreClient,
    private val orderClient: OrderClient,
    private val aiRequestContext: AiRequestContext,
    private val objectMapper: ObjectMapper
) {

    fun recommend(req: RecommendRequest): RecommendInfo {
        val token = aiRequestContext.jwt ?: error("JWT not set in AiRequestContext")

        // Fetch products and orders in parallel
        val productsFuture = CompletableFuture.supplyAsync { storeClient.listProducts(req.storeId, token) }
        val ordersFuture   = CompletableFuture.supplyAsync { runCatching { orderClient.getUserOrders(token) }.getOrElse { emptyList() } }

        val products = productsFuture.get()
        val orders   = ordersFuture.get()

        val menuText = products.filter { it.status }.joinToString("\n") {
            "${it.name} | ${it.description} | ₩${it.price}"
        }

        val storeName = "Store #${req.storeId}"   // name is on StoreInfo; tools fetch it separately if needed

        val (userMessage, showPreferencePicker) = buildUserMessage(
            products   = products,
            orders     = orders.map { order -> order.items.map { it.productName } }.flatten(),
            categories = req.categoryPreferences,
            storeName  = storeName,
            menuText   = menuText
        )

        val raw = runCatching {
            chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content()!!
        }.getOrElse { e ->
            log.warn(e) { "Anthropic call failed for storeId=${req.storeId}" }
            return RecommendInfo(emptyList(), showPreferencePicker)
        }

        return runCatching {
            val parsed = objectMapper.readTree(raw)
            val items = parsed["recommendations"].map { node ->
                RecommendedItem(
                    productName = node["productName"].asText(),
                    reason      = node["reason"].asText()
                )
            }
            RecommendInfo(items, showPreferencePicker)
        }.getOrElse { e ->
            log.warn(e) { "Failed to parse Claude JSON response for storeId=${req.storeId}: $raw" }
            RecommendInfo(emptyList(), showPreferencePicker)
        }
    }

    private fun buildUserMessage(
        products: List<ProductInfo>,
        orders: List<String>,
        categories: List<String>,
        storeName: String,
        menuText: String
    ): Pair<String, Boolean> {
        return when {
            orders.isNotEmpty() -> {
                val pastOrders = orders.distinct().take(20).joinToString(", ")
                Pair(
                    """
                    User's past orders: $pastOrders
                    Store: $storeName
                    Menu:
                    $menuText
                    """.trimIndent(),
                    false
                )
            }
            categories.isNotEmpty() -> {
                Pair(
                    """
                    User's food preferences: ${categories.joinToString(", ")}
                    Store: $storeName
                    Menu:
                    $menuText
                    """.trimIndent(),
                    true
                )
            }
            else -> {
                Pair(
                    """
                    No preference data available. Recommend 3 varied popular items.
                    Store: $storeName
                    Menu:
                    $menuText
                    """.trimIndent(),
                    true
                )
            }
        }
    }
}
