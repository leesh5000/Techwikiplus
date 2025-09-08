package me.helloc.techwikiplus.user.dto.response

import java.time.Instant

data class UserProfileResponse(
    val userId: String,
    val email: String,
    val nickname: String,
    val role: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
