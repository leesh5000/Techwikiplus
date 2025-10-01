package me.helloc.techwikiplus.post.dto.response

data class PostScrollResponse(
    val posts: List<PostSummaryResponse>,
    val hasNext: Boolean,
    val nextCursor: String?,
)
