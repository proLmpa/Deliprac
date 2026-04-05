package notification.service

import common.exception.ForbiddenException
import common.exception.NotFoundException
import notification.dto.NotificationResponse
import notification.entity.Notification
import notification.repository.NotificationRepository
import notification.websocket.NotificationWebSocketHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    @Mock private lateinit var notificationRepository: NotificationRepository
    @Mock private lateinit var webSocketHandler: NotificationWebSocketHandler
    @InjectMocks private lateinit var notificationService: NotificationService

    private val userId         = 1L
    private val notificationId = 100L

    private fun makeNotification(userId: Long = this.userId) = Notification(
        id      = notificationId,
        userId  = userId,
        title   = "New Order",
        content = "A new order has arrived.",
        isRead  = false
    )

    // --- create ---

    @Test
    fun `create - happy path saves notification and pushes via WebSocket`() {
        val saved = makeNotification()
        given(notificationRepository.save(any(Notification::class.java))).willReturn(saved)

        val result = notificationService.create(userId, "New Order", "A new order has arrived.")

        assertThat(result.id).isEqualTo(notificationId)
        assertThat(result.title).isEqualTo("New Order")
        assertThat(result.isRead).isFalse()
        then(webSocketHandler).should().push(userId, NotificationResponse.of(saved))
    }

    // --- listByUser ---

    @Test
    fun `listByUser - returns list of notifications`() {
        given(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
            .willReturn(listOf(makeNotification(), makeNotification()))

        val result = notificationService.listByUser(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("New Order")
    }

    @Test
    fun `listByUser - returns empty list when no notifications`() {
        given(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).willReturn(emptyList())

        val result = notificationService.listByUser(userId)

        assertThat(result).isEmpty()
    }

    // --- markRead ---

    @Test
    fun `markRead - happy path sets isRead to true`() {
        val notification = makeNotification()
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification))
        given(notificationRepository.save(any(Notification::class.java))).willReturn(notification)

        notificationService.markRead(notificationId, userId)

        assertThat(notification.isRead).isTrue()
        then(notificationRepository).should().save(notification)
    }

    @Test
    fun `markRead - not found throws NotFoundException`() {
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty())

        assertThatThrownBy { notificationService.markRead(notificationId, userId) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Notification not found")
    }

    @Test
    fun `markRead - wrong user throws ForbiddenException`() {
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(makeNotification(userId = 999L)))

        assertThatThrownBy { notificationService.markRead(notificationId, userId) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }
}
