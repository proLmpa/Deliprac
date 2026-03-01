package com.example.baemin.store.dto

data class StoreResponse(
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
        fun of(info: StoreInfo) = StoreResponse(
            id                 = info.id,
            ownerId            = info.ownerId,
            name               = info.name,
            address            = info.address,
            phone              = info.phone,
            content            = info.content,
            status             = info.status,
            storePictureUrl    = info.storePictureUrl,
            productCreatedTime = info.productCreatedTime,
            openedTime         = info.openedTime,
            closedTime         = info.closedTime,
            closedDays         = info.closedDays,
            createdAt          = info.createdAt,
            updatedAt          = info.updatedAt
        )
    }
}
