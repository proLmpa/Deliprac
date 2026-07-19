package ai.dto

data class RecommendedItem(
    val productName: String,
    val reason: String
)

// showPreferencePicker = true when no order history exists (Branch B or C).
// Frontend renders FoodPreferencePicker when this is true.
data class RecommendInfo(
    val recommendations: List<RecommendedItem>,
    val showPreferencePicker: Boolean
)

data class RecommendResponse(
    val recommendations: List<RecommendedItem>,
    val showPreferencePicker: Boolean
) {
    companion object {
        fun of(info: RecommendInfo) = RecommendResponse(
            recommendations = info.recommendations,
            showPreferencePicker = info.showPreferencePicker
        )
    }
}

data class ChatInfo(val reply: String)

data class ChatResponse(val reply: String) {
    companion object {
        fun of(info: ChatInfo) = ChatResponse(reply = info.reply)
    }
}
