package me.helloc.techwikiplus.common.infrastructure.security.jwt

import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import org.springframework.security.core.AuthenticationException

class UserStatusAuthenticationException(
    val errorCode: UserErrorCode,
    message: String,
) : AuthenticationException(message) {
    constructor(errorCode: UserErrorCode) : this(errorCode, errorCode.name)
}
