package com.example.baemin.repository.product

import com.example.baemin.entity.product.Product

interface ProductRepositoryCustom {
    fun findTopByStoreIdOrderByPopularityDesc(storeId: Long): List<Product>
}
