package com.example.baemin.entity.cart

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "cart_products")
open class CartProduct(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(name = "cart_id", nullable = false)
    val cartId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "unit_price", nullable = false)
    val unitPrice: Int
)
