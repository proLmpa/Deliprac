package store.config

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.CachingConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration

private val log = LoggerFactory.getLogger(CacheConfig::class.java)

@Configuration
@EnableCaching
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper,
    private val storeCacheConfig: StoreCacheConfig,
) : CachingConfigurer {

    override fun errorHandler(): CacheErrorHandler = object : CacheErrorHandler {
        override fun handleCacheGetError(e: RuntimeException, cache: Cache, key: Any) {
            log.warn("Cache GET error [{}/{}]: {}", cache.name, key, e.message)
        }
        override fun handleCachePutError(e: RuntimeException, cache: Cache, key: Any, value: Any?) {
            log.warn("Cache PUT error [{}/{}]: {}", cache.name, key, e.message)
        }
        override fun handleCacheEvictError(e: RuntimeException, cache: Cache, key: Any) {
            log.warn("Cache EVICT error [{}/{}]: {}", cache.name, key, e.message)
        }
        override fun handleCacheClearError(e: RuntimeException, cache: Cache) {
            log.warn("Cache CLEAR error [{}]: {}", cache.name, e.message)
        }
    }

    private fun redisObjectMapper(): ObjectMapper {
        val ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .build()
        return (objectMapper as JsonMapper).rebuild()
            .activateDefaultTypingAsProperty(ptv, DefaultTyping.NON_FINAL, "@class")
            .build()
    }

    private fun jsonSerializer(): RedisSerializationContext.SerializationPair<Any> =
        RedisSerializationContext.SerializationPair.fromSerializer(
            GenericJacksonJsonRedisSerializer(redisObjectMapper())
        )

    private fun config(ttlMinutes: Long): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(ttlMinutes))
            .serializeValuesWith(jsonSerializer())

    @Bean
    fun cacheManager(): RedisCacheManager =
        RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("stores",            config(storeCacheConfig.redisTtlMinutes))
            .withCacheConfiguration("stores-all",        config(storeCacheConfig.listTtlMinutes))
            .withCacheConfiguration("products",          config(storeCacheConfig.redisTtlMinutes))
            .withCacheConfiguration("products-by-store", config(storeCacheConfig.redisTtlMinutes))
            .build()
}
