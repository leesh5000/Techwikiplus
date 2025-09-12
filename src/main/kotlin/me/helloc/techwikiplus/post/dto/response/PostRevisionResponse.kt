package me.helloc.techwikiplus.post.dto.response

import java.time.Instant

data class PostRevisionResponse(
    val revisionId: String,
    val reviewId: String,
    val authorId: String?,
    val title: String,
    val body: String,
    val tags: List<TagResponse>,
    val submittedAt: Instant,
    val voteCount: Int,
)
