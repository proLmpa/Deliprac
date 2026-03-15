package store.service.product

import common.orThrow
import common.security.UserPrincipal
import common.security.UserRole
import store.dto.product.ProductInfo
import store.repository.product.ProductRepository
import store.repository.store.StoreRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class ProductStatisticsService(
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository
) {

    @Transactional(readOnly = true)
    fun getPopularProducts(storeId: Long, principal: UserPrincipal): List<ProductInfo> {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view store statistics")

        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != principal.id) throw IllegalStateException("Forbidden")

        return productRepository.findTopByStoreIdOrderByPopularityDesc(storeId, 5)
            .map { ProductInfo.of(it) }
    }
}
