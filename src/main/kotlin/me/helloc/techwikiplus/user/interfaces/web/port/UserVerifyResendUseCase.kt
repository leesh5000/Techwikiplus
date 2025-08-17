package me.helloc.techwikiplus.user.interfaces.web.port

import me.helloc.techwikiplus.user.domain.model.Email

interface UserVerifyResendUseCase {
    fun execute(email: Email)
}
