package bff.client

import bff.dto.AiChatRequest
import bff.dto.AiChatResponse
import bff.dto.AiRecommendRequest
import bff.dto.AiRecommendResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiClient(@Qualifier("aiRestClient") private val client: RestClient) {

    private val log = KotlinLogging.logger {}

    @CircuitBreaker(name = "ai", fallbackMethod = "recommendFallback")
    fun recommend(request: AiRecommendRequest, token: String): AiRecommendResponse =
        client.post()
            .uri("/api/ai/recommend")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(AiRecommendResponse::class.java)!!

    private fun recommendFallback(request: AiRecommendRequest, token: String, ex: Throwable): AiRecommendResponse {
        log.warn { "[CB] recommend fallback: ${ex.message}" }
        return AiRecommendResponse(emptyList(), showPreferencePicker = true)
    }

    @CircuitBreaker(name = "ai", fallbackMethod = "chatFallback")
    fun chat(request: AiChatRequest, token: String): AiChatResponse =
        client.post()
            .uri("/api/ai/chat")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .body(AiChatResponse::class.java)!!

    private fun chatFallback(request: AiChatRequest, token: String, ex: Throwable): AiChatResponse {
        log.warn { "[CB] chat fallback: ${ex.message}" }
        throw ex
    }
}
