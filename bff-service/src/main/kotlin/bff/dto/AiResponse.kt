package bff.dto

data class AiRecommendedItem(val productName: String, val reason: String)
data class AiRecommendResponse(val recommendations: List<AiRecommendedItem>, val showPreferencePicker: Boolean)
data class AiChatResponse(val reply: String)
