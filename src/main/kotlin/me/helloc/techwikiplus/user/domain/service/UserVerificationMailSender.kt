package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.MailContent
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.model.RegistrationMailTemplate
import me.helloc.techwikiplus.user.domain.model.UserCacheKey
import me.helloc.techwikiplus.user.domain.service.port.CacheStore
import me.helloc.techwikiplus.user.domain.service.port.MailSender
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserVerificationMailSender(
    private val mailSender: MailSender,
    private val cacheStore: CacheStore,
) {
    companion object {
        private val REGISTRATION_CODE_TTL_DURATION = Duration.ofMinutes(5)
    }

    fun send(email: Email) {
        val registrationCode: RegistrationCode = RegistrationCode.generate()
        val mail: MailContent = RegistrationMailTemplate.of(registrationCode)
        mailSender.send(email, mail)
        val registrationCodeKey = UserCacheKey.REGISTRATION_CODE_KEY_PREFIX.keyFormat.format(email.value)
        cacheStore.put(registrationCodeKey, registrationCode.value, REGISTRATION_CODE_TTL_DURATION)
    }
}
