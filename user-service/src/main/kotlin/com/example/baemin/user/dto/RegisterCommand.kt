package com.example.baemin.user.dto

data class RegisterCommand(val email: String, val password: String, val phone: String?, val role: String = "CUSTOMER")
