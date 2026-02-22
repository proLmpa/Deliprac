package com.example.baemin.store.entity

import com.example.baemin.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "stores")
open class Store(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var owner: User,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "store_picture_url", length = 500)
    var storePictureUrl: String? = null,

    @Column(name = "product_created_time", nullable = false)
    var productCreatedTime: LocalTime,

    @Column(name = "opened_time", nullable = false)
    var openedTime: LocalTime,

    @Column(name = "closed_time", nullable = false)
    var closedTime: LocalTime,

    @Column(name = "closed_days", nullable = false, length = 50)
    var closedDays: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
