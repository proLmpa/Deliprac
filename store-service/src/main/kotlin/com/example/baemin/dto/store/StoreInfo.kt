package com.example.baemin.dto.store

import com.example.baemin.entity.store.Store

data class StoreInfo(
    val id: Long,
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
