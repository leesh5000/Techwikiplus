package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserVerifyLockService(
    private val lockManager: LockManager,
) {
    companion object {
        private const val LOCK_PREFIX = "user:verify:"
        private val WAIT_TIME = Duration.ofSeconds(3)
        private val LEASE_TIME = Duration.ofSeconds(10)
    }

    fun <T> executeWithVerificationLock(
        email: Email,
        action: () -> T,
    ): T {
        val lockKey = "$LOCK_PREFIX${email.value}"

        return lockManager.executeWithLock(
            key = lockKey,
            waitTime = WAIT_TIME,
            leaseTime = LEASE_TIME,
            block = action,
        )
    }
}
