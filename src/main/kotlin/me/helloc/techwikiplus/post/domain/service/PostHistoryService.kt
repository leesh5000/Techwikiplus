package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.history.PostHistory
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostHistoryIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostHistoryService(
    private val postHistoryRepository: PostHistoryRepository,
    private val postHistoryIdGenerator: PostHistoryIdGenerator,
    private val clockHolder: ClockHolder,
) {
    fun saveHistory(
        post: Post,
        reviewId: PostReviewId? = null,
        revisionId: PostRevisionId? = null,
        changedBy: Long? = null,
    ): PostHistory {
        val history =
            PostHistory.create(
                id = postHistoryIdGenerator.generate(),
                postId = post.id,
                title = post.title.value,
                body = post.body.value,
                changedAt = clockHolder.now(),
                reviewId = reviewId,
                revisionId = revisionId,
                changedBy = changedBy,
            )
        return postHistoryRepository.save(history)
    }

    @Transactional(readOnly = true)
    fun getHistories(postId: PostId): List<PostHistory> {
        return postHistoryRepository.findByPostId(postId)
    }
}
