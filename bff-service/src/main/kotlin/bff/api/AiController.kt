package bff.api

import bff.client.AiClient
import bff.dto.AiChatRequest
import bff.dto.AiChatResponse
import bff.dto.AiRecommendRequest
import bff.dto.AiRecommendResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AiController(private val aiClient: AiClient) {

    @PostMapping("/api/ai/recommend")
    fun recommend(
        @RequestBody request: AiRecommendRequest,
        httpRequest: HttpServletRequest
    ): AiRecommendResponse =
        aiClient.recommend(request, httpRequest.bearerToken())

    @PostMapping("/api/ai/chat")
    fun chat(
        @RequestBody request: AiChatRequest,
        httpRequest: HttpServletRequest
    ): AiChatResponse =
        aiClient.chat(request, httpRequest.bearerToken())
}
