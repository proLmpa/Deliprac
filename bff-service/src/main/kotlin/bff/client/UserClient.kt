package bff.client

import bff.dto.LoginUserRequest
import bff.dto.RegisterUserRequest
import bff.dto.SuspendUserRequest
import bff.dto.TokenResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class UserClient(@Qualifier("userRestClient") private val client: RestClient) {

    fun signup(request: RegisterUserRequest): Unit =
        client.post()
            .uri("/api/users/signup")
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    fun signin(request: LoginUserRequest): TokenResponse =
        client.post()
            .uri("/api/users/signin")
            .body(request)
            .retrieve()
            .body(TokenResponse::class.java)!!

    fun suspend(request: SuspendUserRequest, token: String): Unit =
        client.put()
            .uri("/api/users/suspend")
            .header("Authorization", token)
            .body(request)
            .retrieve()
            .toBodilessEntity()
            .let {}

    fun withdraw(token: String): Unit =
        client.put()
            .uri("/api/users/me/withdraw")
            .header("Authorization", token)
            .retrieve()
            .toBodilessEntity()
            .let {}
}
