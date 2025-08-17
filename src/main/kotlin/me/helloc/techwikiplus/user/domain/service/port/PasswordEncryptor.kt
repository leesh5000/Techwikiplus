package me.helloc.techwikiplus.user.domain.service.port

import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.RawPassword

interface PasswordEncryptor {
    fun encode(rawPassword: RawPassword): EncodedPassword

    fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean
}
