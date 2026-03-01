package com.example.baemin.dto.store

data class CreateStoreCommand(
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val storePictureUrl: String?,
    val productCreatedTime: Long,
    val openedTime: Long,
    val closedTime: Long,
    val closedDays: String
)
