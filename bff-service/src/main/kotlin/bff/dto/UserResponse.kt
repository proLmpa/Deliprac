package bff.dto

data class TokenResponse(
    val accessToken: String,
    val tokenType: String
)

data class UserIdResponse(val id: Long)
