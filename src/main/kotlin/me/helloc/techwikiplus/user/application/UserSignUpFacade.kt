package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.interfaces.web.port.UserSignUpUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserSignUpFacade(
    private val userRegister: UserRegister,
    private val emailVerifyService: EmailVerifyService,
) : UserSignUpUseCase {
    override fun handle(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        confirmPassword: RawPassword,
    ) {
        val user =
            userRegister.insert(
                email = email,
                nickname = nickname,
                password = password,
                passwordConfirm = confirmPassword,
            )
        emailVerifyService.startVerification(user)
    }
}
