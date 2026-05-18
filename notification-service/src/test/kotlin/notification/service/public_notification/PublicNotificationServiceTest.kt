package notification.service.public_notification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import common.exception.NotFoundException
import notification.dto.public_notification.CreatePublicNotificationRequest
import notification.dto.public_notification.DeactivatePublicNotificationRequest
import notification.dto.public_notification.PublicNotificationResponse
import notification.entity.public_notification.PublicNotification
import notification.repository.public_notification.PublicNotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.BDDMockito.verifyNoInteractions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.Optional
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicNotificationServiceTest {

    @Mock private lateinit var repository: PublicNotificationRepository
    @Mock private lateinit var caffeineCache: Cache<String, Any>
    @Mock private lateinit var stringRedisTemplate: StringRedisTemplate
    @Mock private lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var service: PublicNotificationService

    private val objectMapper = jacksonObjectMapper()
    private val notifId = 1L

    companion object {
        private const val CACHE_KEY = "public-notifications:active"
    }

    @BeforeEach
    fun setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations)
        service = PublicNotificationService(repository, caffeineCache, stringRedisTemplate, CACHE_KEY, 10L)
    }

    private fun makeEntity(): PublicNotification {
        val now = System.currentTimeMillis()
        return PublicNotification(
            id        = notifId,
            title     = "Test Title",
            content   = "Test Content",
            isActive  = true,
            issuedAt  = now,
            expiresAt = now + 86_400_000L,
        )
    }

    private fun makeResponse(): PublicNotificationResponse {
        val now = System.currentTimeMillis()
        return PublicNotificationResponse(
            id        = notifId,
            title     = "Test Title",
            content   = "Test Content",
            isActive  = true,
            issuedAt  = now,
            expiresAt = now + 86_400_000L,
        )
    }

    private fun makeCreateRequest(): CreatePublicNotificationRequest =
        CreatePublicNotificationRequest(
            title     = "Test Title",
            content   = "Test Content",
            expiresAt = System.currentTimeMillis() + 86_400_000L,
        )

    // --- listActive ---

    @Test
    fun `listActive - L1 Caffeine hit returns cached value without hitting Redis or DB`() {
        val cached = listOf(makeResponse())
        given(caffeineCache.getIfPresent(CACHE_KEY)).willReturn(cached)

        val result = service.listActive()

        assertThat(result).isEqualTo(cached)
        verifyNoInteractions(repository)
        verify(valueOperations, org.mockito.Mockito.never()).get(any())
    }

    @Test
    fun `listActive - L2 Redis hit populates L1 and returns without hitting DB`() {
        val response = makeResponse()
        val json = objectMapper.writeValueAsString(listOf(response))
        given(caffeineCache.getIfPresent(CACHE_KEY)).willReturn(null)
        given(valueOperations.get(CACHE_KEY)).willReturn(json)

        val result = service.listActive()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Test Title")
        verify(caffeineCache).put(eq(CACHE_KEY), any())
        verifyNoInteractions(repository)
    }

    @Test
    fun `listActive - DB hit populates both caches and returns`() {
        given(caffeineCache.getIfPresent(CACHE_KEY)).willReturn(null)
        given(valueOperations.get(CACHE_KEY)).willReturn(null)
        given(repository.findAllByIsActiveTrue()).willReturn(listOf(makeEntity()))

        val result = service.listActive()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Test Title")
        verify(valueOperations).set(eq(CACHE_KEY), any(), eq(10L), eq(TimeUnit.MINUTES))
        verify(caffeineCache).put(eq(CACHE_KEY), any())
    }

    @Test
    fun `listActive - empty DB result populates caches with empty list`() {
        given(caffeineCache.getIfPresent(CACHE_KEY)).willReturn(null)
        given(valueOperations.get(CACHE_KEY)).willReturn(null)
        given(repository.findAllByIsActiveTrue()).willReturn(emptyList())

        val result = service.listActive()

        assertThat(result).isEmpty()
        verify(valueOperations).set(eq(CACHE_KEY), any(), eq(10L), eq(TimeUnit.MINUTES))
    }

    // --- create ---

    @Test
    fun `create - saves entity and evicts both caches`() {
        val entity = makeEntity()
        given(repository.save(any(PublicNotification::class.java))).willReturn(entity)

        val result = service.create(makeCreateRequest())

        assertThat(result.title).isEqualTo("Test Title")
        assertThat(result.isActive).isTrue()
        verify(stringRedisTemplate).delete(CACHE_KEY)
        verify(caffeineCache).invalidate(CACHE_KEY)
    }

    // --- deactivate ---

    @Test
    fun `deactivate - sets isActive false and evicts both caches`() {
        val entity = makeEntity()
        given(repository.findById(notifId)).willReturn(Optional.of(entity))
        given(repository.save(any(PublicNotification::class.java))).willReturn(entity)

        service.deactivate(DeactivatePublicNotificationRequest(notifId))

        assertThat(entity.isActive).isFalse()
        verify(stringRedisTemplate).delete(CACHE_KEY)
        verify(caffeineCache).invalidate(CACHE_KEY)
    }

    @Test
    fun `deactivate - not found throws NotFoundException`() {
        given(repository.findById(notifId)).willReturn(Optional.empty())

        assertThatThrownBy { service.deactivate(DeactivatePublicNotificationRequest(notifId)) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Public notification not found")
    }
}
