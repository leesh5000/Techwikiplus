package me.helloc.techwikiplus.post.dto.response

import java.time.Instant

data class PostReviewResponse(
    val reviewId: String,
    val postId: String,
    val startedAt: Instant,
    val deadline: Instant,
    val status: String,
)
