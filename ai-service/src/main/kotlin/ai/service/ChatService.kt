package ai.service

import ai.dto.ChatInfo
import ai.dto.ChatMessage
import ai.dto.ChatRequest
import ai.tools.BaeminTools
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatClient: ChatClient,
    private val baeminTools: BaeminTools
) {

    fun chat(req: ChatRequest): ChatInfo {
        // Cap history to last 10 messages regardless of what the client sends
        val capped = req.messages.takeLast(10)

        val history = capped.map { msg: ChatMessage ->
            when (msg.role) {
                "assistant" -> AssistantMessage(msg.content)
                else        -> UserMessage(msg.content)
            }
        }

        val reply = chatClient.prompt()
            .messages(history)
            .tools(baeminTools)
            .call()
            .content()
            ?: ""

        return ChatInfo(reply)
    }
}
