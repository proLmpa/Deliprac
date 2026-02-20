package com.example.baemin.user.api

import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.dto.TokenResponse
import com.example.baemin.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    val userService: UserService
) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@RequestBody request: RegisterUserRequest): Map<String, UUID> {
        val id = userService.register(request)
        return mapOf("id" to id)
    }

    @PostMapping("/signin")
    fun signin(@RequestBody request: LoginUserRequest): TokenResponse {
        return userService.login(request)
    }
}
