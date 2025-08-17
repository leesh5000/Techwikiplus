package me.helloc.techwikiplus.user.interfaces.web.port

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import java.time.Instant

interface MyProfileUseCase {
    fun execute(): Result

    data class Result(
        val userId: UserId,
        val email: String,
        val nickname: String,
        val role: UserRole,
        val status: UserStatus,
        val createdAt: Instant,
        val modifiedAt: Instant,
    )
}
