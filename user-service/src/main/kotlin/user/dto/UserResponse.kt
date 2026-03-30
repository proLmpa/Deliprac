package user.dto

data class UserIdResponse(val id: Long)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
