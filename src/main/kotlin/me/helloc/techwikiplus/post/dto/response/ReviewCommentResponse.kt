package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.review.ReviewComment

data class ReviewCommentResponse(
    val id: Long,
    val lineNumber: Int,
    val comment: String,
    val type: String,
    val suggestedChange: String,
) {
    companion object {
        fun from(reviewComment: ReviewComment): ReviewCommentResponse {
            return ReviewCommentResponse(
                id = reviewComment.id.value,
                lineNumber = reviewComment.lineNumber,
                comment = reviewComment.comment,
                type = reviewComment.type.name,
                suggestedChange = reviewComment.suggestedChange,
            )
        }
    }
}
