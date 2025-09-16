package me.helloc.techwikiplus.post.dto.request

data class PostRevisionRequest(
    val title: String,
    val body: String,
    val reviewComments: List<ReviewCommentRequest> = emptyList(),
)
