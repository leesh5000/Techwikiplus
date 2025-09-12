package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostReviewId(val value: Long) {
    init {
        if (value <= 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.VALIDATION_ERROR,
                params = arrayOf("postReviewId"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostReviewId) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        fun from(value: String): PostReviewId {
            try {
                return PostReviewId(value.toLong())
            } catch (e: NumberFormatException) {
                throw PostDomainException(
                    postErrorCode = PostErrorCode.INVALID_POST_REVIEW_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
