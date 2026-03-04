package com.example.baemin.service.product

import com.example.baemin.common.orThrow
import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.product.CreateProductRequest
import com.example.baemin.dto.product.ProductInfo
import com.example.baemin.dto.product.UpdateProductRequest
import com.example.baemin.entity.product.Product
import com.example.baemin.repository.product.ProductRepository
import com.example.baemin.repository.store.StoreRepository
import jakarta.transaction.Transactional
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

    @Transactional
    fun listByStore(storeId: Long): List<ProductInfo> =
        productRepository.findAllByStoreId(storeId).map { ProductInfo.of(it) }

    @Transactional
    fun findById(storeId: Long, productId: Long): ProductInfo {
        val product = productRepository.findById(productId).orThrow("Product not found")
        if (product.storeId != storeId) throw IllegalArgumentException("Product not found in this store")
        return ProductInfo.of(product)
    }

    @Transactional
    fun update(storeId: Long, productId: Long, request: UpdateProductRequest, principal: UserPrincipal): ProductInfo {
        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != principal.id) throw IllegalStateException("Forbidden")
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
    fun deactivate(storeId: Long, productId: Long, principal: UserPrincipal) {
        val store = storeRepository.findById(storeId).orThrow("Store not found")
        if (store.userId != principal.id) throw IllegalStateException("Forbidden")
        val product = productRepository.findById(productId).orThrow("Product not found")
        if (product.storeId != storeId) throw IllegalArgumentException("Product not found in this store")
        product.status    = false
        product.updatedAt = System.currentTimeMillis()
        productRepository.save(product)
    }

    @Transactional
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
