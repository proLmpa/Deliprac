package com.example.baemin.user.entity

import com.example.baemin.common.security.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
open class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(unique = true)
    var phone: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.CUSTOMER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long
)
