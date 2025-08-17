package me.helloc.techwikiplus.user.domain.exception

open class UserDomainException(
    val userErrorCode: UserErrorCode,
    val params: Array<out Any?> = emptyArray(),
    cause: Throwable? = null,
) : RuntimeException(userErrorCode.name, cause)
