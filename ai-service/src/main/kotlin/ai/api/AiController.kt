package ai.api

import ai.config.AiRequestContext
import ai.dto.ChatRequest
import ai.dto.ChatResponse
import ai.dto.RecommendRequest
import ai.dto.RecommendResponse
import ai.service.ChatService
import ai.service.RecommendationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AiController(
    private val recommendationService: RecommendationService,
    private val chatService: ChatService,
    private val aiRequestContext: AiRequestContext
) {

    @PostMapping("/api/ai/recommend")
    fun recommend(
        @RequestBody req: RecommendRequest,
        httpRequest: HttpServletRequest
    ): RecommendResponse {
        aiRequestContext.jwt = httpRequest.getHeader("Authorization")
        return RecommendResponse.of(recommendationService.recommend(req))
    }

    @PostMapping("/api/ai/chat")
    fun chat(
        @RequestBody req: ChatRequest,
        httpRequest: HttpServletRequest
    ): ChatResponse {
        aiRequestContext.jwt = httpRequest.getHeader("Authorization")
        return ChatResponse.of(chatService.chat(req))
    }
}
