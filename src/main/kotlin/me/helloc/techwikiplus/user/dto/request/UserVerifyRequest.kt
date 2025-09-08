package me.helloc.techwikiplus.user.dto.request

data class UserVerifyRequest(
    val email: String,
    val registrationCode: String,
)
