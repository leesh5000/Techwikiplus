package me.helloc.techwikiplus.post.domain.model.tag

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class TagName(value: String) {
    val value: String = value.trim().lowercase()

    init {
        if (this.value.isBlank()) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.BLANK_TAG,
                params = arrayOf("tag"),
            )
        }
        if (this.value.length < MIN_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TAG_TOO_SHORT,
                params = arrayOf(this.value, MIN_LENGTH),
            )
        }
        if (this.value.length > MAX_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TAG_TOO_LONG,
                params = arrayOf(this.value, MAX_LENGTH),
            )
        }
        if (!this.value.matches(ALLOWED_PATTERN)) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TAG_CONTAINS_INVALID_CHAR,
                params = arrayOf(this.value),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagName) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 30
        private val ALLOWED_PATTERN = """^[가-힣a-z0-9_-]+$""".toRegex()
    }
}