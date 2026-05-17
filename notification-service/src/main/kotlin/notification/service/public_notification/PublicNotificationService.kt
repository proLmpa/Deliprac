package notification.service.public_notification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Cache
import common.orThrow
import notification.dto.public_notification.CreatePublicNotificationRequest
import notification.dto.public_notification.DeactivatePublicNotificationRequest
import notification.dto.public_notification.PublicNotificationResponse
import notification.entity.public_notification.PublicNotification
import notification.repository.public_notification.PublicNotificationRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
class PublicNotificationService(
    private val repository: PublicNotificationRepository,
    private val caffeineCache: Cache<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate,
) {
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val CACHE_KEY = "public-notifications:active"
        private const val REDIS_TTL_MINUTES = 10L
    }

    @Transactional(readOnly = true)
    fun listActive(): List<PublicNotificationResponse> {
        caffeineCache.getIfPresent(CACHE_KEY)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as List<PublicNotificationResponse>
        }

        stringRedisTemplate.opsForValue().get(CACHE_KEY)?.let { json ->
            val data = objectMapper.readValue<List<PublicNotificationResponse>>(json)
            caffeineCache.put(CACHE_KEY, data)
            return data
        }

        val data = repository.findAllByIsActiveTrue().map { PublicNotificationResponse.of(it) }
        stringRedisTemplate.opsForValue().set(
            CACHE_KEY,
            objectMapper.writeValueAsString(data),
            REDIS_TTL_MINUTES,
            TimeUnit.MINUTES,
        )
        caffeineCache.put(CACHE_KEY, data)
        return data
    }

    @Transactional
    fun create(request: CreatePublicNotificationRequest): PublicNotificationResponse {
        val entity = repository.save(
            PublicNotification(
                title     = request.title,
                content   = request.content,
                expiresAt = request.expiresAt,
            )
        )
        evictCache()
        return PublicNotificationResponse.of(entity)
    }

    @Transactional
    fun deactivate(request: DeactivatePublicNotificationRequest) {
        val notification = repository.findById(request.notificationId)
            .orThrow("Public notification not found")
        notification.isActive = false
        repository.save(notification)
        evictCache()
    }

    private fun evictCache() {
        stringRedisTemplate.delete(CACHE_KEY)
        caffeineCache.invalidate(CACHE_KEY)
    }
}
