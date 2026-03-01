package com.example.baemin.store.dto

import com.example.baemin.store.entity.Store

data class StoreInfo(
    val id: Long,
    val ownerId: Long,
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val status: String,
    val storePictureUrl: String?,
    val productCreatedTime: Long,
    val openedTime: Long,
    val closedTime: Long,
    val closedDays: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(store: Store) = StoreInfo(
            id                 = store.id,
            ownerId            = store.userId,
            name               = store.name,
            address            = store.address,
            phone              = store.phone,
            content            = store.content,
            status             = store.status.name,
            storePictureUrl    = store.storePictureUrl,
            productCreatedTime = store.productCreatedTime,
            openedTime         = store.openedTime,
            closedTime         = store.closedTime,
            closedDays         = store.closedDays,
            createdAt          = store.createdAt,
            updatedAt          = store.updatedAt
        )
    }
}
