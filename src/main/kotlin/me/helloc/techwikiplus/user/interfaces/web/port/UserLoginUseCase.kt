package me.helloc.techwikiplus.user.interfaces.web.port

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserToken

interface UserLoginUseCase {
    fun execute(
        email: Email,
        password: RawPassword,
    ): Result

    data class Result(
        val accessToken: UserToken,
        val refreshToken: UserToken,
        val userId: UserId,
    )
}
