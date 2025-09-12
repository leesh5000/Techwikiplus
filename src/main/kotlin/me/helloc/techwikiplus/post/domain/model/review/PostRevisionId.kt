package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostRevisionId(val value: Long) {
    init {
        if (value <= 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.VALIDATION_ERROR,
                params = arrayOf("postRevisionId"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostRevisionId) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}
