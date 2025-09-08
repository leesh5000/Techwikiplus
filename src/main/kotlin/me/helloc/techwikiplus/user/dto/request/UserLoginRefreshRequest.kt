package me.helloc.techwikiplus.user.dto.request

data class UserLoginRefreshRequest(
    val userId: String,
    val refreshToken: String,
)
