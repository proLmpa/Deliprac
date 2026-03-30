package bff.api

import bff.client.UserClient
import bff.dto.LoginUserRequest
import bff.dto.RegisterUserRequest
import bff.dto.SuspendUserRequest
import bff.dto.TokenResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userClient: UserClient) {

    @PostMapping("/api/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@RequestBody request: RegisterUserRequest) {
        userClient.signup(request)
    }

    @PostMapping("/api/users/signin")
    fun signin(@RequestBody request: LoginUserRequest): TokenResponse =
        userClient.signin(request)

    @PutMapping("/api/users/suspend")
    fun suspend(@RequestBody request: SuspendUserRequest, httpRequest: HttpServletRequest) =
        userClient.suspend(request, httpRequest.bearerToken())

    @PutMapping("/api/users/me/withdraw")
    fun withdraw(httpRequest: HttpServletRequest) =
        userClient.withdraw(httpRequest.bearerToken())
}

fun HttpServletRequest.bearerToken(): String =
    getHeader("Authorization") ?: ""
