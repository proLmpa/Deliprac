package user.api

import common.security.currentUser
import user.dto.LoginCommand
import user.dto.LoginUserRequest
import user.dto.RegisterCommand
import user.dto.RegisterUserRequest
import user.dto.SuspendUserRequest
import user.dto.TokenResponse
import user.service.UserService
import org.springframework.http.HttpStatus
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

    @PutMapping("/api/users/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun suspend(@RequestBody request: SuspendUserRequest) {
        userService.suspend(request.id, currentUser().role)
    }

    @PutMapping("/api/users/me/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw() {
        userService.withdraw(currentUser().id)
    }
}
