package me.helloc.techwikiplus.user.interfaces.web.port

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RegistrationCode

interface UserVerifyUseCase {
    fun execute(
        email: Email,
        code: RegistrationCode,
    )
}
