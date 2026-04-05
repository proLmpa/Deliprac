package notification.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.JwtParser
import notification.dto.NotificationResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationWebSocketHandler(
    private val jwtParser: JwtParser,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<Long, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = resolveUserId(session)
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        sessions[userId] = session
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.entries.removeIf { it.value.id == session.id }
    }

    fun push(userId: Long, notification: NotificationResponse) {
        sessions[userId]?.takeIf { it.isOpen }
            ?.sendMessage(TextMessage(objectMapper.writeValueAsString(notification)))
    }

    private fun resolveUserId(session: WebSocketSession): Long? =
        try {
            val token = session.uri?.query
                ?.split("&")
                ?.firstOrNull { it.startsWith("token=") }
                ?.removePrefix("token=")
                ?: return null
            jwtParser.parseSignedClaims(token).payload.subject.toLong()
        } catch (e: Exception) {
            null
        }
}
