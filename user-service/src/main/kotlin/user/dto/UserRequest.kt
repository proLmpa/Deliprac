package user.dto

data class RegisterUserRequest(
    val email: String,
    val password: String,
    val phone: String?,
    val role: String = "CUSTOMER"
)

data class LoginUserRequest(
    val email: String,
    val password: String
)

data class RegisterCommand(val email: String, val password: String, val phone: String?, val role: String = "CUSTOMER")

data class LoginCommand(val email: String, val password: String)
