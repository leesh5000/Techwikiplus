package me.helloc.techwikiplus.common.infrastructure.security

import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.service.port.PasswordEncryptor
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.stereotype.Component

@Component
class BCryptPasswordEncryptor : PasswordEncryptor {
    private val encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    override fun encode(rawPassword: RawPassword): EncodedPassword {
        // BCrypt has a 72-byte limit
        val passwordToEncode =
            if (rawPassword.value.toByteArray().size > BCRYPT_MAX_BYTES) {
                String(rawPassword.value.toByteArray().take(BCRYPT_MAX_BYTES).toByteArray())
            } else {
                rawPassword.value
            }
        return EncodedPassword(encoder.encode(passwordToEncode))
    }

    override fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean {
        // Apply the same truncation logic for matching
        val passwordToMatch =
            if (rawPassword.value.toByteArray().size > BCRYPT_MAX_BYTES) {
                String(rawPassword.value.toByteArray().take(BCRYPT_MAX_BYTES).toByteArray())
            } else {
                rawPassword.value
            }
        return encoder.matches(passwordToMatch, encodedPassword.value)
    }

    companion object {
        private const val BCRYPT_MAX_BYTES = 72
    }
}
