package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.user.dto.response.UserLoginResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@Service
class UserLoginService(
    private val repository: UserRepository,
    private val encryptor: PasswordEncryptor,
    private val tokenManager: TokenManager,
) {
    fun login(
        email: Email,
        password: RawPassword,
    ): UserLoginResponse {
        val user =
            repository.findBy(email) ?: throw UserDomainException(
                UserErrorCode.USER_NOT_FOUND,
                arrayOf(email.value),
            )
        user.validateUserStatus()
        validatePasswordMatch(password, user, email)
        val accessToken =
            tokenManager.generateAccessToken(
                user.id,
            )
        val refreshToken =
            tokenManager.generateRefreshToken(
                user.id,
            )
        return UserLoginResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            userId = user.id.value.toString(),
            accessTokenExpiresAt = Instant.ofEpochMilli(accessToken.expiresAt).toString(),
            refreshTokenExpiresAt = Instant.ofEpochMilli(refreshToken.expiresAt).toString(),
        )
    }

    private fun validatePasswordMatch(
        password: RawPassword,
        user: User,
        email: Email,
    ) {
        if (!encryptor.matches(password, user.encodedPassword)) {
            throw UserDomainException(UserErrorCode.INVALID_CREDENTIALS, arrayOf(email.value))
        }
    }
}
