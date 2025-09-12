package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostRevisionService(
    private val postRevisionRepository: PostRevisionRepository,
    private val postRevisionIdGenerator: PostRevisionIdGenerator,
    private val clockHolder: ClockHolder,
) {
    fun submitRevision(
        reviewId: PostReviewId,
        title: PostTitle,
        body: PostBody,
        authorId: Long? = null,
    ): PostRevision {
        val revision =
            PostRevision(
                id = postRevisionIdGenerator.generate(),
                reviewId = reviewId,
                authorId = authorId,
                title = title,
                body = body,
                submittedAt = clockHolder.now(),
                voteCount = 0,
            )

        return postRevisionRepository.save(revision)
    }

    @Transactional(readOnly = true)
    fun getRevisions(reviewId: PostReviewId): List<PostRevision> {
        return postRevisionRepository.findByReviewId(reviewId)
    }

    @Transactional(readOnly = true)
    fun getById(revisionId: PostRevisionId): PostRevision {
        return postRevisionRepository.findById(revisionId)
            ?: throw PostDomainException(
                postErrorCode = PostErrorCode.REVISION_NOT_FOUND,
                params = arrayOf(revisionId.value),
            )
    }
}
