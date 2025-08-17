package me.helloc.techwikiplus.user.domain.service.port

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.MailContent

interface MailSender {
    fun send(
        to: Email,
        content: MailContent,
    )
}
