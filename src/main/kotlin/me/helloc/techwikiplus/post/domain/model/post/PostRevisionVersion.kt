package me.helloc.techwikiplus.post.domain.model.post

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostRevisionVersion(val value: Long = 0) {
    init {
        if (value < 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_VERSION_FORMAT,
                params = arrayOf("postRevisionVersion"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostRevisionVersion) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun from(value: String): PostRevisionVersion {
            try {
                return PostRevisionVersion(value.toLong())
            } catch (e: NumberFormatException) {
                throw PostDomainException(
                    postErrorCode = PostErrorCode.INVALID_POST_VERSION_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
