package ai.service

import ai.client.OrderClient
import ai.client.ProductInfo
import ai.client.StoreClient
import ai.config.AiRequestContext
import ai.dto.RecommendInfo
import ai.dto.RecommendRequest
import ai.dto.RecommendedItem
import tools.jackson.databind.ObjectMapper
import common.security.currentUser
import io.github.oshai.kotlinlogging.KotlinLogging
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

private val auditLog = LoggerFactory.getLogger("audit")

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

        // Fetch products, orders, and store info in parallel
        val productsFuture = CompletableFuture.supplyAsync { storeClient.listProducts(req.storeId, token) }
        val ordersFuture   = CompletableFuture.supplyAsync { runCatching { orderClient.getUserOrders(token) }.getOrElse { emptyList() } }
        val storeFuture    = CompletableFuture.supplyAsync { storeClient.findStore(req.storeId, token) }

        val products  = productsFuture.get()
        val orders    = ordersFuture.get()
        val storeName = storeFuture.get().name

        val menuText = products.filter { it.status }.joinToString("\n") {
            "${it.name} | ${it.description} | ₩${it.price}"
        }

        val (userMessage, showPreferencePicker) = buildUserMessage(
            products   = products,
            orders     = orders.map { order -> order.items.map { it.productName } }.flatten(),
            categories = req.categoryPreferences,
            storeName  = storeName,
            menuText   = menuText
        )

        val raw = runCatching {
            chatClient.prompt()
                .options(AnthropicChatOptions.builder().maxTokens(512).temperature(0.3))
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content()!!
        }.getOrElse { e ->
            log.warn(e) { "Anthropic call failed for storeId=${req.storeId}" }
            return RecommendInfo(emptyList(), showPreferencePicker)
        }

        return runCatching {
            val start = raw.indexOf('{')
            val end   = raw.lastIndexOf('}')
            require(start != -1 && end > start) { "No JSON object in Claude response" }
            val parsed = objectMapper.readTree(raw.substring(start, end + 1))
            val items = parsed["recommendations"].map { node ->
                RecommendedItem(
                    productName = node["productName"].textValue() ?: "",
                    reason      = node["reason"].textValue() ?: ""
                )
            }
            val branch = when {
                !showPreferencePicker                  -> "ORDER_HISTORY"
                req.categoryPreferences.isNotEmpty()   -> "CATEGORY_PREF"
                else                                   -> "FALLBACK"
            }
            auditLog.info(
                appendEntries(mapOf("event" to "AI_RECOMMENDATION", "email" to currentUser().email, "storeId" to req.storeId, "storeName" to storeName, "count" to items.size, "branch" to branch)),
                "Customer ${currentUser().email} received ${items.size} AI recommendations at '$storeName' [branch: $branch]"
            )
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
