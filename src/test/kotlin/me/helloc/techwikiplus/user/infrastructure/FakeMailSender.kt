package me.helloc.techwikiplus.user.infrastructure

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.MailContent
import me.helloc.techwikiplus.user.domain.service.port.MailSender
import java.util.concurrent.ConcurrentLinkedQueue

open class FakeMailSender : MailSender {
    data class SentMail(
        val to: Email,
        val content: MailContent,
    )

    private val sentMails = ConcurrentLinkedQueue<SentMail>()

    override fun send(
        to: Email,
        content: MailContent,
    ) {
        sentMails.add(SentMail(to, content))
    }

    fun getSentMails(): List<SentMail> {
        return sentMails.toList()
    }

    fun getLastSentMail(): SentMail? {
        return sentMails.toList().lastOrNull()
    }

    fun getSentMailCount(): Int {
        return sentMails.size
    }

    fun wasSentTo(email: Email): Boolean {
        return sentMails.any { it.to == email }
    }

    fun clear() {
        sentMails.clear()
    }
}
