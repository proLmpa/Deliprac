package store.config

import org.springframework.cache.annotation.EnableCaching
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

@Configuration
@EnableCaching
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper,
    private val storeCacheConfig: StoreCacheConfig,
) {

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
