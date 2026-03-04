package com.example.baemin.repository.order

import com.example.baemin.entity.order.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long>, OrderRepositoryCustom {
    fun findAllByStoreId(storeId: Long): List<Order>
    fun findAllByUserId(userId: Long): List<Order>
}
