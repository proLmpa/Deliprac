package ai.dto

// categoryPreferences: explicit list from localStorage when no order history exists.
// Send an empty list when order history is available — ai-service uses history instead.
data class RecommendRequest(
    val storeId: Long,
    val categoryPreferences: List<String>
)

data class ChatRequest(val messages: List<ChatMessage>)

data class ChatMessage(
    val role: String,   // "user" | "assistant"
    val content: String
)
