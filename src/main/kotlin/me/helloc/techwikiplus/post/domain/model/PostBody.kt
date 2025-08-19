package me.helloc.techwikiplus.post.domain.model

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostBody(value: String) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.BLANK_CONTENT,
                params = arrayOf("content"),
            )
        }
        if (this.value.length < MIN_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.CONTENT_TOO_SHORT,
                params = arrayOf<Any>("content", MIN_LENGTH),
            )
        }
        if (this.value.length > MAX_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.CONTENT_TOO_LONG,
                params = arrayOf<Any>("content", MAX_LENGTH),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostBody) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "PostBody(value=${value.take(50)}${if (value.length > 50) "..." else ""})"
    }

    companion object {
        private const val MIN_LENGTH = 30
        private const val MAX_LENGTH = 50000
    }
}
