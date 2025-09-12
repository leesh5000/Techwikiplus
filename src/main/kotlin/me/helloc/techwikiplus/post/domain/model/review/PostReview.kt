package me.helloc.techwikiplus.post.domain.model.review

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.PostId
import java.time.Duration
import java.time.Instant

class PostReview(
    val id: PostReviewId,
    val postId: PostId,
    val startedAt: Instant,
    val deadline: Instant,
    val status: PostReviewStatus,
    val winningRevisionId: PostRevisionId? = null,
    val startedBy: Long? = null,
) {
    fun isExpired(now: Instant): Boolean {
        return now.isAfter(deadline) && status == PostReviewStatus.IN_REVIEW
    }

    fun complete(winningRevisionId: PostRevisionId): PostReview {
        if (status != PostReviewStatus.IN_REVIEW) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_REVIEW_STATE,
                params = arrayOf(status),
            )
        }
        return PostReview(
            id = id,
            postId = postId,
            startedAt = startedAt,
            deadline = deadline,
            status = PostReviewStatus.COMPLETED,
            winningRevisionId = winningRevisionId,
            startedBy = startedBy,
        )
    }

    companion object {
        val REVIEW_DURATION: Duration = Duration.ofHours(72)

        fun create(
            id: PostReviewId,
            postId: PostId,
            now: Instant,
            startedBy: Long? = null,
        ): PostReview {
            return PostReview(
                id = id,
                postId = postId,
                startedAt = now,
                deadline = now.plus(REVIEW_DURATION),
                status = PostReviewStatus.IN_REVIEW,
                winningRevisionId = null,
                startedBy = startedBy,
            )
        }
    }
}
