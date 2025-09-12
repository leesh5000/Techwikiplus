package me.helloc.techwikiplus.post.interfaces.web.dto

import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import java.time.Instant

data class PostRevisionResponse(
    val id: Long,
    val reviewId: Long,
    val title: String,
    val body: String,
    val authorId: Long?,
    val submittedAt: Instant,
    val voteCount: Int,
) {
    companion object {
        fun from(revision: PostRevision): PostRevisionResponse {
            return PostRevisionResponse(
                id = revision.id.value,
                reviewId = revision.reviewId.value,
                title = revision.title.value,
                body = revision.body.value,
                authorId = revision.authorId,
                submittedAt = revision.submittedAt,
                voteCount = revision.voteCount,
            )
        }
    }
}
