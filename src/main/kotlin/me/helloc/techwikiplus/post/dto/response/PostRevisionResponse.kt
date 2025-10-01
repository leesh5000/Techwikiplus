package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.post.Post
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
    val originalTitle: String? = null,
    val originalBody: String? = null,
    val reviewComments: List<ReviewCommentResponse> = emptyList(),
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
                reviewComments = revision.reviewComments.map { ReviewCommentResponse.from(it) },
            )
        }

        fun from(
            revision: PostRevision,
            originalPost: Post,
        ): PostRevisionResponse {
            return PostRevisionResponse(
                id = revision.id.value,
                reviewId = revision.reviewId.value,
                title = revision.title.value,
                body = revision.body.value,
                authorId = revision.authorId,
                submittedAt = revision.submittedAt,
                voteCount = revision.voteCount,
                originalTitle = originalPost.title.value,
                originalBody = originalPost.body.value,
                reviewComments = revision.reviewComments.map { ReviewCommentResponse.from(it) },
            )
        }
    }
}
