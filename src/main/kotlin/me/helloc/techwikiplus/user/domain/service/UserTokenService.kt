package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserToken
import me.helloc.techwikiplus.user.domain.service.port.TokenManager
import org.springframework.stereotype.Service

@Service
class UserTokenService(
    private val tokenManager: TokenManager,
) {
    fun generateTokens(userId: UserId): TokenPair {
        val accessToken = tokenManager.generateAccessToken(userId)
        val refreshToken = tokenManager.generateRefreshToken(userId)

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    data class TokenPair(
        val accessToken: UserToken,
        val refreshToken: UserToken,
    )
}
