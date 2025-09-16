package me.helloc.techwikiplus.post.dto.response

data class ReviewCommentTypeResponse(
    val value: String,
    val description: String,
) {
    companion object {
        fun from(
            type: String,
            description: String,
        ): ReviewCommentTypeResponse {
            return ReviewCommentTypeResponse(
                value = type,
                description = description,
            )
        }
    }
}
