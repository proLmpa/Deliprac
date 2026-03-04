package com.example.baemin.repository.product

import com.example.baemin.entity.product.Product
import com.example.baemin.entity.product.QProduct
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    override fun findTopByStoreIdOrderByPopularityDesc(storeId: Long): List<Product> {
        val product = QProduct.product
        return queryFactory
            .selectFrom(product)
            .where(product.storeId.eq(storeId))
            .orderBy(product.popularity.desc())
            .fetch()
    }
}
