package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class UserVerifyResendService(
    private val userReader: UserReader,
    private val userVerificationMailSender: UserVerificationMailSender,
) {
    fun verifyResend(email: Email): User {
        val user: User = userReader.getPendingUser(email)
        userVerificationMailSender.send(email)
        return user
    }
}
