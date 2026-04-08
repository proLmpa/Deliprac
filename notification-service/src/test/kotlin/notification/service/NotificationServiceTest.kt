package notification.service

import common.exception.ForbiddenException
import common.exception.NotFoundException
import notification.dto.CreateNotificationRequest
import notification.entity.Notification
import notification.entity.NotificationType
import notification.repository.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    @Mock private lateinit var notificationRepository: NotificationRepository
    @InjectMocks private lateinit var notificationService: NotificationService

    private val userId         = 1L
    private val otherUserId    = 2L
    private val notificationId = 10L

    private fun makeNotification(read: Boolean = false): Notification {
        val now = System.currentTimeMillis()
        return Notification(
            id        = notificationId,
            userId    = userId,
            type      = NotificationType.NEW_ORDER,
            title     = "Test Title",
            content   = "Test Content",
            isRead    = read,
            issuedAt  = now,
            expiry    = now + Notification.MIN_EXPIRY_MILLIS + 1000L,
            createdAt = now
        )
    }

    private fun makeRequest(): CreateNotificationRequest {
        val now = System.currentTimeMillis()
        return CreateNotificationRequest(
            recipientId = userId,
            type        = NotificationType.NEW_ORDER,
            title       = "Test Title",
            content     = "Test Content",
            expiry      = now + Notification.MIN_EXPIRY_MILLIS + 1000L
        )
    }

    // --- createNotification ---

    @Test
    fun `createNotification - happy path saves and returns notification`() {
        val notification = makeNotification()
        given(notificationRepository.save(any(Notification::class.java))).willReturn(notification)

        val result = notificationService.createNotification(makeRequest())

        assertThat(result.id).isEqualTo(notificationId)
        assertThat(result.title).isEqualTo("Test Title")
        assertThat(result.isRead).isFalse()
    }

    // --- listMyNotifications ---

    @Test
    fun `listMyNotifications - unreadOnly=false returns all notifications`() {
        val notifications = listOf(makeNotification(read = true), makeNotification())
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(notifications)

        val result = notificationService.listMyNotifications(userId, unreadOnly = false)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `listMyNotifications - unreadOnly=true returns only unread`() {
        val unread = listOf(makeNotification())
        given(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)).willReturn(unread)

        val result = notificationService.listMyNotifications(userId, unreadOnly = true)

        assertThat(result).hasSize(1)
        assertThat(result[0].isRead).isFalse()
    }

    // --- markRead ---

    @Test
    fun `markRead - happy path marks notification as read`() {
        val notification = makeNotification()
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification))
        given(notificationRepository.save(any(Notification::class.java))).willReturn(notification)

        val result = notificationService.markRead(userId, notificationId)

        assertThat(result.isRead).isTrue()
    }

    @Test
    fun `markRead - notification not found throws NotFoundException`() {
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty())

        assertThatThrownBy { notificationService.markRead(userId, notificationId) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Notification not found")
    }

    @Test
    fun `markRead - wrong user throws ForbiddenException`() {
        val notification = makeNotification()
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification))

        assertThatThrownBy { notificationService.markRead(otherUserId, notificationId) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }

    // --- markAllRead ---

    @Test
    fun `markAllRead - marks all unread notifications as read`() {
        val unread = listOf(makeNotification(), makeNotification())
        given(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)).willReturn(unread)
        given(notificationRepository.saveAll(any<List<Notification>>())).willReturn(unread)

        notificationService.markAllRead(userId)

        assertThat(unread).allMatch { it.isRead }
    }
}
