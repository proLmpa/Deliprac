package com.example.baemin.repository.product

import com.example.baemin.entity.product.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom {
    fun findAllByStoreId(storeId: Long): List<Product>
}
