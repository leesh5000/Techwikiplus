package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ReviewHistoryResponse(
    val reviewId: Long,
    val startedAt: Instant,
    val deadline: String,
    val status: PostReviewStatus,
    val winningRevisionId: Long?,
    val completedAt: String?,
) {
    companion object {
        private val ISO_8601_FORMATTER = DateTimeFormatter.ISO_INSTANT

        fun from(review: PostReview): ReviewHistoryResponse {
            return ReviewHistoryResponse(
                reviewId = review.id.value,
                startedAt = review.startedAt,
                deadline = formatInstant(review.deadline),
                status = review.status,
                winningRevisionId = review.winningRevisionId?.value,
                completedAt = calculateCompletedAt(review),
            )
        }

        private fun formatInstant(instant: Instant): String {
            return instant.atOffset(ZoneOffset.UTC).format(ISO_8601_FORMATTER)
        }

        private fun calculateCompletedAt(review: PostReview): String? {
            return when (review.status) {
                PostReviewStatus.COMPLETED -> {
                    // If review is completed, use the deadline as a proxy for completion time
                    // In a real scenario, you might want to track actual completion time
                    formatInstant(review.deadline)
                }
                else -> null
            }
        }
    }
}
