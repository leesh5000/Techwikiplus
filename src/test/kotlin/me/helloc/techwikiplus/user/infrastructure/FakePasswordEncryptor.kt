package me.helloc.techwikiplus.user.infrastructure

import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.service.port.PasswordEncryptor

class FakePasswordEncryptor : PasswordEncryptor {
    override fun encode(rawPassword: RawPassword): EncodedPassword {
        return EncodedPassword("encoded_${rawPassword.value}")
    }

    override fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean {
        return encodedPassword.value == "encoded_${rawPassword.value}"
    }
}
