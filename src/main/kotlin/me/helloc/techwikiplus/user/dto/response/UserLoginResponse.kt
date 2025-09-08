package me.helloc.techwikiplus.user.dto.response

data class UserLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    // ISO-8601 표준
    val accessTokenExpiresAt: String,
    // ISO-8601 표준
    val refreshTokenExpiresAt: String,
)
