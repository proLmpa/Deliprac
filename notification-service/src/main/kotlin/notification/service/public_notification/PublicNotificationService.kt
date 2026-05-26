package notification.service.public_notification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Cache
import common.orThrow
import notification.config.NotificationCacheConfig
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
    private val notifiCacheConfig: NotificationCacheConfig,
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional(readOnly = true)
    fun listActive(): List<PublicNotificationResponse> {
        caffeineCache.getIfPresent(notifiCacheConfig.key)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as List<PublicNotificationResponse>
        }

        stringRedisTemplate.opsForValue().get(notifiCacheConfig.key)?.let { json ->
            val data = objectMapper.readValue<List<PublicNotificationResponse>>(json)
            caffeineCache.put(notifiCacheConfig.key, data)
            return data
        }

        val data = repository.findAllByIsActiveTrue().map { PublicNotificationResponse.of(it) }
        stringRedisTemplate.opsForValue().set(
            notifiCacheConfig.key,
            objectMapper.writeValueAsString(data),
            notifiCacheConfig.redisTtlMinutes,
            TimeUnit.MINUTES,
        )
        caffeineCache.put(notifiCacheConfig.key, data)
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
        stringRedisTemplate.delete(notifiCacheConfig.key)
        caffeineCache.invalidate(notifiCacheConfig.key)
    }
}
