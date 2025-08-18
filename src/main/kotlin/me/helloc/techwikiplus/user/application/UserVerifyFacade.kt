package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.user.domain.service.UserModifier
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserVerifyLockService
import me.helloc.techwikiplus.user.interfaces.web.port.UserVerifyUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserVerifyFacade(
    private val userReader: UserReader,
    private val emailVerifyService: EmailVerifyService,
    private val userModifier: UserModifier,
    private val userVerifyLockService: UserVerifyLockService,
) : UserVerifyUseCase {
    override fun handle(
        email: Email,
        code: RegistrationCode,
    ) {
        // 분산 락을 사용하여 동일 이메일의 동시 인증 요청 방지
        userVerifyLockService.executeWithVerificationLock(email) {
            // 1. 회원 가입 대기중인 사용자 조회
            val pendingUser: User = userReader.getPendingUser(email)

            // 2. 인증 코드가 유효한지 확인
            emailVerifyService.verify(pendingUser.email, code)

            // 3. 사용자 상태를 ACTIVE로 변경
            userModifier.activate(pendingUser)
        }
    }
}
