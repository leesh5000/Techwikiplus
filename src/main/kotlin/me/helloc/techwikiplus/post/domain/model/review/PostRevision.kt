package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import java.time.Instant

class PostRevision(
    val id: PostRevisionId,
    val reviewId: PostReviewId,
    // nullable for anonymous users
    val authorId: Long?,
    val title: PostTitle,
    val body: PostBody,
    val reviewComments: List<ReviewComment> = emptyList(),
    val submittedAt: Instant,
    val voteCount: Int = 0,
) {
    fun incrementVoteCount(): PostRevision {
        return PostRevision(
            id = id,
            reviewId = reviewId,
            authorId = authorId,
            title = title,
            body = body,
            reviewComments = reviewComments,
            submittedAt = submittedAt,
            voteCount = voteCount + 1,
        )
    }

    fun decrementVoteCount(): PostRevision {
        return PostRevision(
            id = id,
            reviewId = reviewId,
            authorId = authorId,
            title = title,
            body = body,
            reviewComments = reviewComments,
            submittedAt = submittedAt,
            voteCount = maxOf(0, voteCount - 1),
        )
    }
}
