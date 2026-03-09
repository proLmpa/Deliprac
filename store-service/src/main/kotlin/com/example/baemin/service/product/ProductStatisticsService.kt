package com.example.baemin.service.product

import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.product.ProductInfo
import com.example.baemin.repository.product.ProductRepository
import com.example.baemin.repository.store.StoreRepository
import org.springframework.stereotype.Service

@Service
class ProductStatisticsService(
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository
) {

    fun getPopularProducts(storeId: Long, principal: UserPrincipal): List<ProductInfo> {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can view store statistics")

        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != principal.id) throw IllegalStateException("Forbidden")

        return productRepository.findTopByStoreIdOrderByPopularityDesc(storeId, 5)
            .map { ProductInfo.of(it) }
    }
}
