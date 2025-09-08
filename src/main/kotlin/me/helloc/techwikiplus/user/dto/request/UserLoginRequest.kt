package me.helloc.techwikiplus.user.dto.request

data class UserLoginRequest(
    val email: String,
    val password: String,
)
