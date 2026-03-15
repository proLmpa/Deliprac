package store.repository.product

import store.entity.product.Product

interface ProductRepositoryCustom {
    fun findTopByStoreIdOrderByPopularityDesc(storeId: Long, limit: Long): List<Product>
}
