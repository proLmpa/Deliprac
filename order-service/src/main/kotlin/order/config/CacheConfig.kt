package order.config

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
    private val orderCacheConfig: OrderCacheConfig,
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

    @Bean
    fun cacheManager(): RedisCacheManager =
        RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration(
                "orders-by-user",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(orderCacheConfig.redisTtlMinutes))
                    .serializeValuesWith(jsonSerializer())
            ).build()
}
