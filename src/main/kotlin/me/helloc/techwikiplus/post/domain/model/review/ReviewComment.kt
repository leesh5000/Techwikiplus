package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

data class ReviewComment(
    val id: ReviewCommentId,
    val lineNumber: Int,
    val comment: String,
    val type: ReviewCommentType,
    val suggestedChange: String,
) {
    companion object {
        private const val MAX_COMMENT_LENGTH = 15000
        private const val MAX_SUGGESTED_CHANGE_LENGTH = 5000
    }

    init {
        if (comment.isBlank()) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.BLANK_REVIEW_COMMENT,
            )
        }

        if (comment.length > MAX_COMMENT_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.REVIEW_COMMENT_TOO_LONG,
                params = arrayOf(MAX_COMMENT_LENGTH),
            )
        }

        if (lineNumber <= 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_LINE_NUMBER,
                params = arrayOf(lineNumber),
            )
        }

        if (suggestedChange.isBlank()) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.BLANK_SUGGESTED_CHANGE,
            )
        }

        if (suggestedChange.length > MAX_SUGGESTED_CHANGE_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.SUGGESTED_CHANGE_TOO_LONG,
                params = arrayOf(MAX_SUGGESTED_CHANGE_LENGTH),
            )
        }
    }
}
