package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.MailContent
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.model.RegistrationMailTemplate
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.service.port.CacheStore
import me.helloc.techwikiplus.user.domain.service.port.MailSender
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class EmailVerifyService(
    private val mailSender: MailSender,
    private val cacheStore: CacheStore,
) {
    companion object {
        private fun getRegistrationCodeKey(email: String): String = "user:registration_code:$email"

        private val REGISTRATION_CODE_TTL_DURATION = Duration.ofMinutes(5)
    }

    fun startVerification(user: User) {
        val registrationCode: RegistrationCode = RegistrationCode.generate()
        val mail: MailContent = RegistrationMailTemplate.of(registrationCode)
        mailSender.send(user.email, mail)
        store(registrationCode, user.email)
    }

    @Throws(UserDomainException::class)
    fun verify(
        email: Email,
        registrationCode: RegistrationCode,
    ) {
        val registrationCodeKey = getRegistrationCodeKey(email.value)
        val code: String =
            cacheStore.get(registrationCodeKey)
                ?: throw UserDomainException(UserErrorCode.REGISTRATION_EXPIRED, arrayOf(email.value))
        if (code != registrationCode.value) {
            throw UserDomainException(UserErrorCode.CODE_MISMATCH)
        }
        cacheStore.delete(registrationCodeKey)
    }

    private fun store(
        registrationCode: RegistrationCode,
        email: Email,
    ) {
        val registrationCodeKey = getRegistrationCodeKey(email.value)
        cacheStore.put(registrationCodeKey, registrationCode.value, REGISTRATION_CODE_TTL_DURATION)
    }
}
