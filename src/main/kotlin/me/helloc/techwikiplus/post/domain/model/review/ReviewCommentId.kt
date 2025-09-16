package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class ReviewCommentId(val value: Long) {
    init {
        if (value <= 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_REVIEW_COMMENT_ID_FORMAT,
                params = arrayOf("reviewCommentId"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReviewCommentId) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        fun from(value: String): ReviewCommentId {
            try {
                return ReviewCommentId(value.toLong())
            } catch (e: NumberFormatException) {
                throw PostDomainException(
                    postErrorCode = PostErrorCode.INVALID_REVIEW_COMMENT_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
