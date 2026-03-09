package com.example.baemin.user.api

import com.example.baemin.common.security.currentUser
import com.example.baemin.user.dto.LoginCommand
import com.example.baemin.user.dto.LoginUserRequest
import com.example.baemin.user.dto.RegisterCommand
import com.example.baemin.user.dto.RegisterUserRequest
import com.example.baemin.user.dto.TokenResponse
import com.example.baemin.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    val userService: UserService
) {

    @PostMapping("/api/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@RequestBody request: RegisterUserRequest) {
        userService.register(RegisterCommand(request.email, request.password, request.phone, request.role))
    }

    @PostMapping("/api/users/signin")
    fun signin(@RequestBody request: LoginUserRequest): TokenResponse {
        val token = userService.login(LoginCommand(request.email, request.password))
        return TokenResponse(accessToken = token)
    }

    @PutMapping("/api/users/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun suspend(@PathVariable id: Long) {
        userService.suspend(id, currentUser())
    }

    @PutMapping("/api/users/me/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw() {
        userService.withdraw(currentUser())
    }
}
