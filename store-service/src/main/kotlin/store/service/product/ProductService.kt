package store.service.product

import common.orThrow
import common.security.UserPrincipal
import common.security.UserRole
import store.dto.product.CreateProductRequest
import store.dto.product.ProductInfo
import store.dto.product.UpdateProductRequest
import store.entity.product.Product
import store.repository.product.ProductRepository
import store.repository.store.StoreRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository
) {

    @Transactional
    fun create(storeId: Long, request: CreateProductRequest, principal: UserPrincipal): ProductInfo {
        if (principal.role != UserRole.OWNER) throw IllegalStateException("Only OWNER can create products")

        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != principal.id) throw IllegalStateException("Forbidden")

        val now = System.currentTimeMillis()
        val product = Product(
            id                = 0L,
            storeId           = storeId,
            name              = request.name,
            description       = request.description,
            price             = request.price,
            productPictureUrl = request.productPictureUrl,
            popularity        = 0,
            status            = true,
            createdAt         = now,
            updatedAt         = now
        )

        return ProductInfo.of(productRepository.save(product))
    }

    @Transactional(readOnly = true)
    fun listByStore(storeId: Long): List<ProductInfo> =
        productRepository.findAllByStoreId(storeId).map { ProductInfo.of(it) }

    @Transactional(readOnly = true)
    fun findById(storeId: Long, productId: Long): ProductInfo {
        val product = productRepository.findById(productId).orThrow("Product not found")
        if (product.storeId != storeId) throw IllegalArgumentException("Product not found in this store")

        return ProductInfo.of(product)
    }

    @Transactional
    fun update(storeId: Long, productId: Long, request: UpdateProductRequest, userId: Long): ProductInfo {
        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != userId) throw IllegalStateException("Forbidden")

        val product = productRepository.findById(productId).orThrow("Product not found")
        if (product.storeId != storeId) throw IllegalArgumentException("Product not found in this store")

        product.name              = request.name
        product.description       = request.description
        product.price             = request.price
        product.productPictureUrl = request.productPictureUrl
        product.updatedAt         = System.currentTimeMillis()

        return ProductInfo.of(productRepository.save(product))
    }

    @Transactional
    fun deactivate(storeId: Long, productId: Long, userId: Long) {
        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != userId) throw IllegalStateException("Forbidden")

        val product = productRepository.findById(productId).orThrow("Product not found")
        if (product.storeId != storeId) throw IllegalArgumentException("Product not found in this store")

        product.status    = false
        product.updatedAt = System.currentTimeMillis()

        productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun getById(productId: Long): ProductInfo {
        val product = productRepository.findById(productId).orThrow("Product not found")
        return ProductInfo.of(product)
    }

    @Transactional
    fun incrementPopularity(productId: Long, delta: Int) {
        val product = productRepository.findById(productId).orThrow("Product not found")

        product.popularity += delta
        product.updatedAt  = System.currentTimeMillis()

        productRepository.save(product)
    }
}
