package me.helloc.techwikiplus.post.interfaces.web.dto

import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import java.time.Instant

data class PostReviewResponse(
    val id: Long,
    val postId: Long,
    val startedAt: Instant,
    val deadline: Instant,
    val status: PostReviewStatus,
    val winningRevisionId: Long?,
) {
    companion object {
        fun from(review: PostReview): PostReviewResponse {
            return PostReviewResponse(
                id = review.id.value,
                postId = review.postId.value,
                startedAt = review.startedAt,
                deadline = review.deadline,
                status = review.status,
                winningRevisionId = review.winningRevisionId?.value,
            )
        }
    }
}
