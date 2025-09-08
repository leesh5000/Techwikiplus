package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserCacheKey
import me.helloc.techwikiplus.user.domain.model.UserLockKey
import me.helloc.techwikiplus.user.domain.service.port.CacheStore
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration.ofSeconds
import java.time.Instant

@Transactional
@Service
class UserVerifyService(
    private val clockHolder: ClockHolder,
    private val repository: UserRepository,
    private val lockManager: LockManager,
    private val cacheStore: CacheStore,
    private val userReader: UserReader,
) {
    fun verifyEmail(
        email: Email,
        registrationCode: RegistrationCode,
    ) {
        val lockKey = UserLockKey.VERIFY_EMAIL_LOCK_KEY_PREFIX.keyFormat.format(email.value)
        val waitTime = ofSeconds(3)
        val leaseTime = ofSeconds(10)
        lockManager.executeWithLock(lockKey, waitTime, leaseTime) {
            val user: User = userReader.getPendingUser(email)
            verifyRegistrationCode(email, registrationCode)
            activateUser(user)
        }
    }

    private fun activateUser(user: User) {
        val updatedAt: Instant = clockHolder.now()
        val activatedUser: User = user.activate(updatedAt)
        repository.save(activatedUser)
    }

    private fun verifyRegistrationCode(
        email: Email,
        registrationCode: RegistrationCode,
    ) {
        val registrationCodeKey = UserCacheKey.REGISTRATION_CODE_KEY_PREFIX.keyFormat.format(email.value)
        val code: String =
            cacheStore.get(registrationCodeKey)
                ?: throw UserDomainException(UserErrorCode.REGISTRATION_EXPIRED, arrayOf(email.value))
        if (code != registrationCode.value) {
            throw UserDomainException(UserErrorCode.CODE_MISMATCH)
        }
        cacheStore.delete(registrationCodeKey)
    }
}
