package store.repository.product

import store.entity.product.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom {
    fun findAllByStoreId(storeId: Long): List<Product>
}
