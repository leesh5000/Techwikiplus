package me.helloc.techwikiplus.post.domain.model.history

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import java.time.Instant

data class PostHistory(
    val id: PostHistoryId,
    val postId: PostId,
    val title: String,
    val body: String,
    val changeType: PostChangeType,
    val changedAt: Instant,
    val reviewId: PostReviewId? = null,
    val revisionId: PostRevisionId? = null,
    val changedBy: Long? = null,
) {
    companion object {
        fun create(
            id: PostHistoryId,
            postId: PostId,
            title: String,
            body: String,
            changeType: PostChangeType,
            changedAt: Instant,
            reviewId: PostReviewId? = null,
            revisionId: PostRevisionId? = null,
            changedBy: Long? = null,
        ): PostHistory {
            return PostHistory(
                id = id,
                postId = postId,
                title = title,
                body = body,
                changeType = changeType,
                changedAt = changedAt,
                reviewId = reviewId,
                revisionId = revisionId,
                changedBy = changedBy,
            )
        }
    }
}
