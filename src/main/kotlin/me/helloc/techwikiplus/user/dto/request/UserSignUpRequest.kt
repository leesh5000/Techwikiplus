package me.helloc.techwikiplus.user.dto.request

data class UserSignUpRequest(
    val email: String,
    val nickname: String,
    val password: String,
    val confirmPassword: String,
)
