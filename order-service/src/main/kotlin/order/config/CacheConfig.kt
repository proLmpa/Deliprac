package order.config

import order.dto.order.OrderResponse
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper,
    private val orderCacheConfig: OrderCacheConfig,
) {
    private fun <T : Any> typedSerializer(type: JavaType): RedisSerializer<T> = object : RedisSerializer<T> {
        override fun serialize(value: T?): ByteArray =
            value?.let { objectMapper.writeValueAsBytes(it) } ?: byteArrayOf()

        override fun deserialize(bytes: ByteArray?): T? =
            bytes?.takeIf { it.isNotEmpty() }?.let { objectMapper.readValue(it, type) }
    }

    @Bean
    fun cacheManager(): RedisCacheManager {
        val tf = objectMapper.typeFactory
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(orderCacheConfig.redisTtlMinutes))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    typedSerializer(tf.constructCollectionType(List::class.java, OrderResponse::class.java))
                )
            )
        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("orders-by-user", config)
            .build()
    }
}
