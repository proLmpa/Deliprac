package com.example.baemin.store.dto

import java.time.LocalTime

data class CreateStoreRequest(
    val name: String,
    val address: String,
    val phone: String,
    val content: String,
    val storePictureUrl: String?,
    val productCreatedTime: LocalTime,
    val openedTime: LocalTime,
    val closedTime: LocalTime,
    val closedDays: String
)
