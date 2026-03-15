package store.dto.store

import store.entity.store.Store

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
    val averageRating: Double,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(store: Store, averageRating: Double) = StoreInfo(
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
            averageRating      = averageRating,
            createdAt          = store.createdAt,
            updatedAt          = store.updatedAt
        )
    }
}

data class StoreResponse(
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
    val averageRating: Double,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun of(info: StoreInfo) = StoreResponse(
            id                 = info.id,
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
            averageRating      = info.averageRating,
            createdAt          = info.createdAt,
            updatedAt          = info.updatedAt
        )
    }
}
