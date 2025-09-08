package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserToken
import me.helloc.techwikiplus.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.user.dto.response.UserLoginResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@Service
class UserLoginRefreshService(
    private val userReader: UserReader,
    private val tokenManager: TokenManager,
) {
    fun refreshLogin(
        userId: UserId,
        refreshToken: String,
    ): UserLoginResponse {
        val activeUser = userReader.getActiveUser(userId)
        activeUser.validateUserStatus()
        tokenManager.validateRefreshToken(activeUser.id, refreshToken)
        val accessToken: UserToken = tokenManager.generateAccessToken(activeUser.id)
        val refreshToken: UserToken = tokenManager.generateRefreshToken(activeUser.id)
        return UserLoginResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            userId = activeUser.id.value.toString(),
            accessTokenExpiresAt = Instant.ofEpochMilli(accessToken.expiresAt).toString(),
            refreshTokenExpiresAt = Instant.ofEpochMilli(refreshToken.expiresAt).toString(),
        )
    }
}
