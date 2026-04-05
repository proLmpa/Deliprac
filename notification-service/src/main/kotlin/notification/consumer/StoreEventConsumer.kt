package notification.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import notification.entity.StoreOwnerProjection
import notification.repository.StoreOwnerProjectionRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class StoreCreatedEvent(val storeId: Long, val ownerUserId: Long)

@Component
class StoreEventConsumer(
    private val storeOwnerProjectionRepository: StoreOwnerProjectionRepository,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(topics = ["store.created"], groupId = "notification-service")
    @Transactional
    fun onStoreCreated(message: String) {
        val event = objectMapper.readValue(message, StoreCreatedEvent::class.java)
        storeOwnerProjectionRepository.save(StoreOwnerProjection(storeId = event.storeId, ownerUserId = event.ownerUserId))
    }
}
