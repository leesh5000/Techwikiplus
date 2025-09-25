package me.helloc.techwikiplus.post.dto.request

data class ReviewCommentRequest(
    val lineNumber: Int,
    val comment: String,
    // "INACCURACY" or "NEEDS_IMPROVEMENT"
    val type: String,
    val suggestedChange: String,
)
