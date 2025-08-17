package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.interfaces.web.port.UserVerifyResendUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserVerifyResendFacade(
    private val userReader: UserReader,
    private val emailVerifyService: EmailVerifyService,
) : UserVerifyResendUseCase {
    override fun execute(email: Email) {
        // 사용자 조회
        val pendingUser: User = userReader.getPendingUser(email)
        // 인증 메일 재전송
        emailVerifyService.startVerification(pendingUser)
    }
}
