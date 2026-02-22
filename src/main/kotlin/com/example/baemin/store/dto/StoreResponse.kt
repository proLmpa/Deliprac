package com.example.baemin.store.dto

import com.example.baemin.store.entity.Store
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class StoreResponse(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val storePictureUrl: String?,
    val productCreatedTime: LocalTime,
    val openedTime: LocalTime,
    val closedTime: LocalTime,
    val closedDays: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(store: Store) = StoreResponse(
            id                 = store.id!!,
            ownerId            = store.owner.id!!,
            name               = store.name,
            address            = store.address,
            phone              = store.phone,
            content            = store.content,
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
