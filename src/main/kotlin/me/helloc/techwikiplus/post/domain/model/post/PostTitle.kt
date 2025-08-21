package me.helloc.techwikiplus.post.domain.model.post

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostTitle(value: String) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.BLANK_TITLE,
                params = arrayOf("title"),
            )
        }
        if (this.value.length > MAX_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TITLE_TOO_LONG,
                params = arrayOf<Any>("title", MAX_LENGTH),
            )
        }
        if (!this.value.matches(ALLOWED_PATTERN)) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TITLE_CONTAINS_INVALID_CHAR,
                params = arrayOf("title"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostTitle) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "PostTitle(value=$value)"
    }

    companion object {
        private const val MAX_LENGTH = 150
        private val ALLOWED_PATTERN = """^[가-힣a-zA-Z0-9\s!?.,:()\-_'"/@#$%^&*+=\[\]{}|\\~`]+$""".toRegex()
    }
}
