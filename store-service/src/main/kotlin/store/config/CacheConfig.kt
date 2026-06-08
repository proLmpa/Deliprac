package store.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import store.dto.product.ProductInfo
import store.dto.store.StoreInfo
import tools.jackson.databind.JavaType
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper,
    private val storeCacheConfig: StoreCacheConfig,
) {
    private fun <T : Any> typedSerializer(type: JavaType): RedisSerializer<T> = object : RedisSerializer<T> {
        override fun serialize(value: T?): ByteArray =
            value?.let { objectMapper.writeValueAsBytes(it) } ?: byteArrayOf()

        override fun deserialize(bytes: ByteArray?): T? =
            bytes?.takeIf { it.isNotEmpty() }?.let { objectMapper.readValue(it, type) }
    }

    private fun config(type: JavaType, ttlMinutes: Long): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(ttlMinutes))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(typedSerializer(type)))

    @Bean
    fun cacheManager(): RedisCacheManager {
        val tf = objectMapper.typeFactory
        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("stores",
                config(tf.constructType(StoreInfo::class.java), storeCacheConfig.redisTtlMinutes))
            .withCacheConfiguration("stores-all",
                config(tf.constructCollectionType(List::class.java, StoreInfo::class.java), storeCacheConfig.listTtlMinutes))
            .withCacheConfiguration("products",
                config(tf.constructType(ProductInfo::class.java), storeCacheConfig.redisTtlMinutes))
            .withCacheConfiguration("products-by-store",
                config(tf.constructCollectionType(List::class.java, ProductInfo::class.java), storeCacheConfig.redisTtlMinutes))
            .build()
    }
}
