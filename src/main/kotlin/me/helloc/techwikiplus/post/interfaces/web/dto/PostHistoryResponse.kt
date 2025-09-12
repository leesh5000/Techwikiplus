package me.helloc.techwikiplus.post.interfaces.web.dto

import me.helloc.techwikiplus.post.domain.model.history.PostHistory
import java.time.Instant

data class PostHistoryResponse(
    val id: Long,
    val postId: Long,
    val title: String,
    val body: String,
    val changedAt: Instant,
    val reviewId: Long?,
    val revisionId: Long?,
    val changedBy: Long?,
) {
    companion object {
        fun from(history: PostHistory): PostHistoryResponse {
            return PostHistoryResponse(
                id = history.id.value,
                postId = history.postId.value,
                title = history.title,
                body = history.body,
                changedAt = history.changedAt,
                reviewId = history.reviewId?.value,
                revisionId = history.revisionId?.value,
                changedBy = history.changedBy,
            )
        }
    }
}
