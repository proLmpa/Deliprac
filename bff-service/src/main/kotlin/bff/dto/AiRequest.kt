package bff.dto

data class AiRecommendRequest(val storeId: Long, val categoryPreferences: List<String>)
data class AiChatMessage(val role: String, val content: String)
data class AiChatRequest(val messages: List<AiChatMessage>)
